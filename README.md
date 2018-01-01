# config-client
配置中心客户端</br>
通过PropertySourcesPlaceholderConfigurer继承类静态bean管理项目所有配置，直接嵌入其他架包内的propertyConfigurer。</br>
利用反射记录下项目所有使用了@Value注解的属性以及持有该属性的bean，每当配置更新时刷新这个bean与属性。</br>
redis实现最基本的mq，接受来自服务端的最新配置推送，本架包内的configurer暴露一个refresh接口，既可以指定特定文件手动执行刷新也可以自动刷新。</br>
提供一个可以灵活获取仓库配置的类。</br>

## 使用方式
* application.yml文件配置`core.config.client.configUris`以及`core.config.client.configBranch`分别指定配置文件与仓库分支，其中多个配置文件则以","相连
* 公共配置请置于仓库项目的Master分支下common-config.properties文件下，这样所有使用本架包的项目都可以实时获取最新配置
* 不要忘了在启动类上添加本架包的包扫描路径

## 架包适配策略
* core/config/client/下两个文件用于对接服务端以及redis，根据不同环境的服务端搭建酌情写入定制化配置
