# CaptureInterfaceXposed
### 大致步骤

1. 安装Magisk，并使用Magisk接管root
2. 安装LSP_Mod，（对比LSP增加命令行功能）
3. 安装界面信息收集
4. 收集数据（使用向）

### 详细步骤

1. 安装Magisk，并使用Magisk接管root。

   - 下载地址：https://wwtp.lanzoul.com/i8cBU0tvvgud 密码:29wd

   - 下载安装后，打开Magisk，授予Magisk Root权限

   - 重新打开Magisk，点击右上角安装，选择第二个“直接安装（推荐）”，再点击右边开始按钮。等待安装完成，显示 all done，最后点击重启，会自动重启手机。 

     ![image-20230425122427318](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230425122427318.png)

   - 重启手机后打开Magisk（一般）会显示：“检测到不属于Magisk的su文件，请删除其他超级用户程序”，可以不用管，如果要解决的话，可以利用Magisk隐藏其他su文件。

   - 底部Tab（4个按钮），都可以点击，就正常了。

     ![image-20230516111641347](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230516111641347.png)

2. 安装LSP_Mod，（对比LSP增加命令行功能）

   - Github：https://github.com/mywalkb/LSPosed_mod/

     下载地址：https://github.com/mywalkb/LSPosed_mod/releases/download/v1.8.6_mod/LSPosed-v1.8.6_mod-6821-zygisk-release.zip

   - 下载完成后，打开Magisk，点击模块，点击从本地安装，找到下载的LSP_Mod，点击即可安装为Magisk模块，安装完成后需要重启。

     ![image-20230425111619390](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230425111619390.png)

   - 重启完成后，打开Magisk，如以上状态则安装成功。

     ![image-20230516111706092](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230516111706092.png)

   - 打开LSPosed，点击“齿轮（设置）”，打开Enable CLI，并将Session timeout in minutes 设为Disabled。

     ![image-20230425112611710](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230425112611710.png)

3. 安装界面信息收集

   - Code：https://github.com/QX-Tao/CaptureInterfaceXposed

     下载地址：https://wwtp.lanzoul.com/iz2YS0walvfa 密码:9gor

   - 打开界面信息收集，授予运行权限，即可进入应用。应用主界面及设置界面如下。

     ![image-20230516102556225](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230516102556225.png)

4. 收集数据（使用向）

   - 软件介绍：进入软件后，会提示需要权限，安卓版本不同，所需权限亦不同；用户授予所有权限后，方可进入应用。软件所实现的主要功能为收集界面数据，提供普通模式和LSP注入模式，用户可以在设置中自由切换工作模式。其他已实现功能：查看收集结果、同步数据文件、多语言切换、主题切换等。

     - LSP注入模式下，用户需要首先授予应用ROOT权限，软件会自动加载LSP作用域，（此时可能需要重启手机），待软件提示“LSP注入(模块已激活)”，即表示LSP注入模式开启成功；若是提示“LSP注入(模块未激活)”，则表示LSP作用域加载失败，此时无法收集数据，用户可以打开LSPosed查看激活状态，需确保模块作用域包含“系统框架(android)”，并重启手机。在LSP注入模式正常工作时，用户需要点击“选择应用”，选择需要收集数据（Hook）的应用程序，在选择应用后，点击“开启服务”，再点击“开始收集”，即可收集数据。

       ![image-20230516105738429](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230516105738429.png)

     - 普通模式下，用户需要手动对待测APP进行插桩，此时不再需要“选择应用”，点击“开启服务”，再点击“开始收集”，软件会自动返回桌面，此时打开已插桩的待测APP，即可收集数据。

       ![image-20230516105400724](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230516105400724.png)

   - 在正常进入收集数据时，屏幕左上角会展现浮窗，其具备两个按钮，先点按钮1（蓝色），表明开始收集，如果接下来收集的还是同一个界面，那就点击按钮2（绿色）收集，直到切换到了另一个页面，就再次点击按钮1（蓝色）重启逻辑。

     用户可以长按按钮2（绿色）打开功能面板，此时可以切换收集路径，亦可以切换收集应用（普通模式不支持）。
   
     蓝色按钮创建文件夹并保存文件；绿色按钮仅保存文件，未创建文件夹时点击无效。
   
     ![image-20230516111256971](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230516111256971.png)
   
   - 收集到的数据保存到：/storage/emulated/0/Download/CaptureInterface/，并以包名区分应用程序，以点击时间区分单元数据。
