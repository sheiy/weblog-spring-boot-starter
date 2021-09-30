# 使用方式
1. application.properties中配置文件设置打印请求用序列号`logging.pattern.console=%clr(%d{${LOG_DATEFORMAT_PATTERN:yyyy-MM-dd HH:mm:ss.SSS}}){faint} %X{SN} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:%wEx}`

2. 设置打印log的Controller目录或类的日志级别为`Debug` 如：`logging.level.site.ownw.demo.C=debug`

# 日志效果如下：
```
2021-09-30 14:55:37.685 7480f21d0314478ea54726972ff1436b -DEBUG 1864 --- [nio-8080-exec-1] c.g.s.seed.controller.TestController     : HTTP请求处理开始:[handler=com.github.sheiy.seed.controller.TestController.test,ip=127.0.0.1,request=[]]
2021-09-30 14:55:37.688 7480f21d0314478ea54726972ff1436b -DEBUG 1864 --- [nio-8080-exec-1] c.g.s.seed.controller.TestController     : HTTP请求处理结束:[handler=com.github.sheiy.seed.controller.TestController.test,ip=127.0.0.1,response=String(hello world)]

```
