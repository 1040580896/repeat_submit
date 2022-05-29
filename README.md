对于前端传来的 JSON 数据，我们在服务端基本上都是通过 IO 流来解析，如果是古老的 Servlet，那么我们直接解析 IO 流；如果是在 SpringMVC 中，我们往往通过 @RequestBody 注解来解析。

如果通过 IO 流来解析参数，默认情况下，IO 流读一次就结束了，就没有了。而往往有些场景，需要我们多次读取参数，我举一个例子：

> ❝
>
> 接口幂等性的处理，同一个接口，在短时间内接收到相同参数的请求，接口可能会拒绝处理。那么在判断的时候，就需要先把请求的参数提取出来进行判断，如果是 JSON 参数，此时就会有问题，参数提前取出来了，将来在接口中再去获取 JSON 参数，就会发现没有了。





# 1. 问题演示

假设我现在有一个处理接口幂等性的拦截器，在这个拦截器中，我需要先获取到请求的参数，然后进行比对等等（接口幂等性的具体实现细节下篇文章和大家分享），我这里先简单模拟一下，比如我们项目中有如下拦截器：

```java
@Component
public class RepeatSubmitInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("request.getReader().readLine() = " + request.getReader().readLine());
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
```

在这个拦截器中先把请求的参数拎出来，瞅一眼。通过 IO 流读取出来的参数最大特点是一次性，也就是读一次就失效了。



然后我们配置一下这个拦截器：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Autowired
    RepeatSubmitInterceptor repeatSubmitInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
    }
```



最后再来看看 Controller 接口：

```
@RestController
public class HelloController {
    @PostMapping("/hello")
    public void hello(@RequestBody String msg) throws IOException {
        System.out.println("msg = " + msg);
    }
}
```

在接口参数上我们加了 @RequestBody 注解，这个底层也是通过 IO 流来读取数据的，但是由于 IO 流在拦截器中已经被读取过一次了，所以到了接口中再去读取就会出错。报错信息如下：

![图片](img/640.png)

然而很多时候，我们希望 IO 流能够被多次读取，那么怎么办呢



# 2. 问题解决

这里我们可以利用`装饰者模式`对 HttpServletRequest 的功能进行增强，具体做法也很简单，我们重新定义一个 HttpServletRequest：

```java
public class RepeatableReadRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] bytes;

    public RepeatableReadRequestWrapper(HttpServletRequest request, HttpServletResponse response) throws IOException {
        super(request);
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        bytes = request.getReader().readLine().getBytes();
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return  new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {

            }

            @Override
            public int read() throws IOException {
                return bais.read();
            }

            @Override
            public int available() throws IOException {
                return bytes.length;
            }
        } ;
    }
}
```



这段代码并不难，很好懂。

首先在构造 RepeatedlyRequestWrapper 的时候，就通过 IO 流将数据读取出来并存入到一个 byte 数组中，然后重写 getReader 和 getInputStream 方法，在这两个读取 IO 流的方法中，都从 byte 数组中返回 IO 流数据出来，这样就实现了反复读取了。



接下来我们定义一个过滤器，让这个装饰后的 Request 生效：

```java
public class RepeatableRequestFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        if(StringUtils.startsWithIgnoreCase(request.getContentType(),"application/json")){
            RepeatableReadRequestWrapper requestWrapper = new RepeatableReadRequestWrapper(request, (HttpServletResponse) servletResponse);
            filterChain.doFilter(requestWrapper,servletResponse);
            return;
        }
        filterChain.doFilter(servletRequest,servletResponse);

    }
}
```



判断一下，如果请求数据类型是 JSON 的话，就把 HttpServletRequest “偷梁换柱”改为 RepeatedlyRequestWrapper，然后让过滤器继续往下走。

最后再配置一下这个过滤器：

```java
   @Bean
    FilterRegistrationBean<RepeatableRequestFilter> repeatableRequestFilterFilterRegistrationBean(){
        FilterRegistrationBean<RepeatableRequestFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RepeatableRequestFilter());
        bean.addUrlPatterns("/*");
        return bean;
    }
```



> 过滤器比拦截器先执行

好啦大功告成。

以后，我们的 JSON 数据就可以通过 IO 流反复读取了。