# CaptureInterfaceXposed
### 大致步骤

1. 安装Magisk，并使用Magisk接管root
2. 安装LSP_Mod，（对比LSP增加命令行功能）
3. 安装界面信息收集
4. 收集数据 

### 详细步骤

1. 安装Magisk，并使用Magisk接管root。

   - 下载地址：https://wwtp.lanzoul.com/i8cBU0tvvgud 密码:29wd

   - 下载安装后，打开Magisk，授予Magisk Root权限

   - 重新打开Magisk，点击右上角安装，选择第二个“直接安装（推荐）”，再点击右边开始按钮。等待安装完成，显示 all done，最后点击重启，会自动重启手机。 

     ![image-20230425122427318](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230425122427318.png)

   - 重启手机后打开Magisk（一般）会显示：“检测到不属于Magisk的su文件，请删除其他超级用户程序”，可以不用管，如果要解决的话，可以利用Magisk隐藏其他su文件。

   - 底部Tab（4个按钮），都可以点击，就正常了。

     <img src="https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230425110942909.png" alt="image-20230425110942909" style="zoom:50%;" />

2. 安装LSP_Mod，（对比LSP增加命令行功能）

   - Github：https://github.com/mywalkb/LSPosed_mod/

     下载地址：https://github.com/mywalkb/LSPosed_mod/releases/download/v1.8.6_mod/LSPosed-v1.8.6_mod-6821-zygisk-release.zip

   - 下载完成后，打开Magisk，点击模块，点击从本地安装，找到下载的LSP_Mod，点击即可安装为Magisk模块，安装完成后需要重启。

     ![image-20230425111619390](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230425111619390.png)

   - 重启完成后，打开Magisk，如以上状态则安装成功。

     <img src="https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230425111851107.png" alt="image-20230425111851107" style="zoom:33%;" />

   - 打开LSPosed，点击“齿轮（设置）”，打开Enable CLI，并将Session timeout in minutes 设为Disabled。

     ![image-20230425112611710](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230425112611710.png)

3. 安装界面信息收集

   - Code：https://github.com/QX-Tao/CaptureInterfaceXposed

     下载地址：https://wwtp.lanzoul.com/iTyfN0tvy6hi 密码:hb9c

   - 打开界面信息收集，授予root权限，点击确定，重启完成后即可。 

     <img src="https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230425113017187.png" alt="image-20230425113017187" style="zoom: 50%;" />

4. 收集数据

   - 点击选择应用，选择需要收集数据（Hook）的应用程序，点击HOOK并打开应用即可。

     程序会自动加载LSP作用域，不需要到LSP中手动勾选。

     ![image-20230425113647667](https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230425113647667.png)

   - 收集数据时有两个按钮，先点按钮1（蓝色），表明开始收集，如果接下来收集的还是同一个界面，那就点击按钮2（绿色）收集，直到切换到了另一个页面，就再次点击按钮1（蓝色）重启逻辑。

     蓝色按钮创建文件夹并保存文件；绿色按钮仅保存文件，未创建文件夹时点击无效。

     <img src="https://gitee.com/pan_yitao/cloud-image/raw/master/image/image-20230425113910138.png" alt="image-20230425113910138" style="zoom:33%;" />

   - 收集到的数据保存到：/storage/emulated/0/Download/界面信息收集/，并以包名区分应用程序，以点击时间区分单元数据。
