---
title: VR迷宫01-OpenGL与GoogleVRSDK
date: 2019-09-30 14:25:02
mathjax: true
tags:
---



作为《虚拟现实技术》课程的第一次小作业，我们要在Android平台上利用Google VR SDK for Android开发一款简单的走迷宫游戏。VR眼镜的话我们选择Cardboard，感觉纸壳版的寿命堪忧，于是在某宝上找到了塑胶版，拿在手里结实多了，价格也与纸壳板差不多。

<!-- more -->

## 编译运行Google VR SDK样例程序

直接下载最新的[1.200版本的SDK](https://github.com/googlevr/gvr-android-sdk/releases/download/v1.200/gvr-android-sdk-1.200.zip)，使用Android Studio(Ver3.1.4)打开整个工程项目，里面含有开发SDK以及若干样例程序。我们编译并运行``sdk-hellovr``，发现gradle的过程中报错了。

搜索之后得到的结果出于某些原因目前1.200版本不能正常工作，将所有有关的依赖库从1.200版本改回1.190版本就可以正常编译运行了。

``sdk-hellovr``实现了一个房间场景，同时有一个浮动物体，如果你在视线正对着它的情况下触摸屏幕(按下Cardboard的控制按钮)，他就会消失并刷新在场景内一个新的随机位置。

看了一下可以发现代码里面有很多都与OpenGL有关，事实上依赖库``android.opengl.GLES20``是安卓官方为嵌入式系统上的OpenGL提供的支持。所以我们先回顾一下OpenGL的一些基础知识。

## OpenGL基础知识

### 坐标系统

OpenGL的绘制是以图元为单位进行的，以一个三角面片为例，对于它上面的一个点$p$，我们要知道最后我们在屏幕上看到它在哪个位置。

这经历了如下几个过程：

**局部空间**：$p$相对于所在面片某固定点的坐标。

**世界空间**：$p$在三维空间的绝对坐标。

**观察空间**：引入一个视点，以视点为原点，以观察方向为$z$轴正方向，以视点上方为$y$轴正方向，以视点右侧为$x$轴正方向建立一个坐标系，则$p$在该坐标系下的坐标。

**裁剪空间**：我们将观察空间中的坐标投影成为标准化设备坐标，同时只有标准化设备坐标满足$-1\leq x,y,z\leq 1$的点才是可见的(会被渲染)。标准化设备坐标层面上的裁剪操作限制了我们能看到哪些点，故而称为裁剪空间。

> 我们常使用一个投影矩阵完成观察坐标到裁剪坐标的映射，但得到的裁剪坐标并不是$(x,y,z)$而是$(x,y,z,w)$，该点距离视点的距离越远，$w$的值就越大，同时满足$|x|,|y|,|z|\leq w$。
>
> 如果使用正射投影矩阵，则所有点的$w$值均为1；如果考虑透视效应使用透视投影矩阵，则$w$的值会根据点到视点的距离不同而变化。
>
> 而标准化设备坐标为$(\frac{x}{w},\frac{y}{w},\frac{z}{w})$，从$(x,y,z,w)$变为标准化设备坐标的过程称为透视除法。这一步是顶点着色器自动完成的。

**屏幕空间**：(假使我们可以看到它)$p$在屏幕上对应的像素点的坐标。本质上就是把$p$从三维的裁剪空间投影到一个二维平面上。

参考文献：[LearnOpenGL坐标系统](https://learnopengl-cn.github.io/01%20Getting%20started/08%20Coordinate%20Systems/)，[LearnOpenGL摄像机](https://learnopengl-cn.github.io/01%20Getting%20started/09%20Camera/)

### 着色器

渲染过程主要是由OpenGL的图形渲染管线(Graphics Pipeline)实现的，它接受一组3D坐标(与参数)，并将他们转化为屏幕上的有色2D输出。管线可以被划分为几个阶段，后一个阶段以前一个阶段的输出为输入。而其中的某些阶段我们可以用OpenGL着色器语言(GLSL)实现着色器小程序来自定义它们的功能。

![图形渲染管线图示](https://learnopengl-cn.github.io/img/01/04/pipeline.png)

图中蓝色的顶点着色器(vertex shader)、几何着色器(geometry shader)、片段着色器(fragment shader)是我们可以自定义的阶段。其中几何着色器我们通常使用OpenGL的默认实现。

其中顶点着色器的主要任务是给output类型的内部变量``gl_Position``变量赋值$(x,y,z,w)$，随后顶点着色器会自动完成透视除法变换为标准化设备坐标。裁剪操作可能分布在后续的几个阶段中，但总之不用我们写一行代码。

片段着色器的主要任务是计算一个像素的最终颜色，并放在output类型的内部变量``gl_FlagColor``中，注意它是``RGBA``格式的，以``vec4``的形式给出。

参考文献：[GLSL入门大全](https://github.com/wshxbqq/GLSL-Card)，[LearnOpenGL着色器](https://learnopengl-cn.github.io/01 Getting started/05 Shaders/)

## 样例代码分析

核心的``HelloVrActivity``类继承``GvrActivity``并实现``GvrView.StereoRenderer``接口，我们也可以选择实现``GvrView.Renderer``接口，但那样更加灵活也更加复杂，所以我们选择前者。

在函数``onCreate()``中，主要是

对用到的数组进行了初始化，并建立起了与布局中的``gvrView``的相互联系，并对``gvrView``进行了设置。还对音频引擎进行了初始化。

### onSurfaceCreated

``onSurfaceCreated()``是``StereoRenderer``要求实现的，主要完成整个场景的搭建。

1. 清理颜色缓冲

2. 编译并链接硬编码的顶点着色器、片段着色器，并获取要传入其中的变量的位置参数，方便在管线处理中传值。

   ``attribute``全局，只读。只能存在于vertex shader中，一般用于保存顶点与法线数据，可在数据缓冲区中读取数据。

   ``varying``用于在vertex shader(输出到varying)和fragment shader(从varying输入)之间传递数据。

   ``uniform``全局，只读。在整个管线运行过程中不能被改变。

   这里传入的``a_Position``是从缓冲中读到的模型顶点的局部坐标。``a_UV``是从缓冲中读到的该顶点在模型贴图中的``UV``坐标。

   > 为了给模型上色方便，我们将模型的各个图元展开成一个平面形成一张贴图，模型上的一个顶点在这张贴图中的位置就是UV坐标。

   这两个都是从缓冲中读取到的，因此使用变量限定符``attribute``。

   传入的``u_MVP``则是将局部坐标变换为透视除法之前的标准化设备坐标的矩阵。公式为：$$M_{\text{Perspect}}\cdot M_{\text{View}}\cdot M_{\text{Model}}$$，其中
   $$M_{\text{Model}}$$将局部坐标变为世界坐标，$$M_{\text{View}}$$将世界坐标变为观察坐标，$$M_{\text{Perspect}}$$即为透视矩阵，将观察坐标变为透视除法之前的标准化设备坐标。注意这里的坐标与变换矩阵都是四维的。

3. 初始化房间场景的$$M_{\text{Model}}$$，只是向$$y$$轴正方向平移了一下。

4. 开一个新线程循环播放背景音乐。

5. 在``updateTargetLocation()``中，更新浮动物体的$M_{\text{Model}}$，也只是简单的平移，随后将音效引擎的发声位置设定为浮动物体所在位置。

6. 加载房间场景的模型及纹理，以及三种浮动物体的模型与纹理。

### onNewFrame

``onNewFrame``是``StereoRenderer``要求实现的，主要作用是在即将绘制一帧之前，通过此时此刻的使用者头部姿态``headTransform``进行某些必要的初始化。这种头部姿态用欧拉角可以很好描述：俯仰角、偏航角、滚转角。这部分可以参考[LearnOpenGL摄像机](https://learnopengl-cn.github.io/01%20Getting%20started/09%20Camera/)。

1. 重置$M_{\text{view}}$矩阵``camera``。

   传入的三个参数分别为视点、视线上某一点、以及视点上面的方向向量。

2. 获取头部姿态的四元数进行音效引擎的接受者设定。

### onDrawEye

``onDrawEye``是``StereoRenderer``要求实现的，应该是在``onNewFrame``之后立即被调用，完成渲染这一帧的工作。至于为什么不将这两个函数合在一起呢？是因为我们要分别对于左眼和右眼分别渲染一帧，现在我们只需接受输入的眼睛类型并进行渲染，其调用细节就不用处理了。

1. 在正式渲染之前，需要启动深度测试，并清空深度缓冲与颜色缓冲。
2. 将$$M_{\text{View}}$$ 矩阵左乘eye transform矩阵，得到针对该传入眼睛的$M_{\text{View}}$。
3. 对于浮动物体，将$$M_{\text{Perspect}},M_{\text{View}},M_{\text{Model}}$$乘起来，将结果保存到``modelViewProjection``中。并绘制浮动物体。
4. 对于房间场景，同理，只不过$M_{\text{Model}}$换成房间场景的。并绘制房间场景。

### onFinishFrame

``onFinishFrame``是``StereoRenderer``要求实现的，作用是在一帧完成渲染之后，在上面再覆盖一些东西。大概是提示信息、菜单之类的应该放在这里进行渲染。这个样例中没有做任何事情。

### onCardboardTrigger

继承自``GvrActivity``，用来定义Cardboard Trigger被按下(手机屏幕被触摸)后的行为。

如果视线正对着浮动物体，则播放对应音效，并调用``hideTarget()``将浮动物体刷新在一个新的位置。

刷新的机理是随机浮动物体到视点的距离、随机浮动物体相对视点的俯仰角(pitch)以及偏航角(yaw)，并随机浮动物体的样式。

利用这些信息，我们更新浮动物体的$M_{\text{Model}}$，并更新用来渲染的模型``curTargetObject``。

那么如何判断视线是否正对着浮动物体呢？我们只考虑浮动物体的模型中心，由于几种可能的浮动物体都是中心对称的，因此局部坐标均为$$(0,0,0)$$，我们将这个坐标先左乘$$M_{\text{Model}}$$，再左乘$M_{\text{View}}$转化为观察坐标，在观察空间内视线方向恒为$$(0,0,-1)$$，只需看视点到观察坐标的向量，与视线方向的夹角是否小于一个阈值即可。

### drawTarget/drawRoom

都是将``onDrawEye()``中预先计算好的``modelViewProjection``矩阵传到vertex shader中的``u_MVP``中用来将局部坐标映射到透视除法之前的标准化设备坐标。但这只是完成了管线的定义，我们还需要给定管线的输入：即模型的顶点、纹理、属性等信息。可以看到，无论是房间场景还是浮动物体，我们都是先调用纹理类``Texture``的``bind``方法，再调用模型类``TexturedMesh``的``draw``方法，这样就完成了模型的渲染。

### Texture

贴图类在构造函数中，主要进行了如下初始化工作：

1. 调用``glGenTextures``创建纹理，第一个参数为创建纹理个数，并将返回的id存储在``textureId[0]``中。
2. 调用``bind``方法，激活纹理单元``GL_TEXTURE0``，并以id``textureId[0]``的方式将刚创建的纹理绑定到``GL_TEXTURE0``。
3. 调用``glTexParameteri``设置纹理的环绕与过滤方式。
4. 读取图片，并调用``texImage2D``将图片输入纹理，同时生成多级渐远纹理。

### TexturedMesh

模型类在构造函数中，主要进行了如下初始化工作：

1. 通过库从``.obj``格式模型中读取``obj``。
2. 通过``ObjData``库从``obj``中读取``indices,vertices,uv``三个buffer。
3. 将输入的``a_Position,a_UV``在管线程序中的位置保存下来，用于传值。

最后在``draw()``中进行绘制时，首先通过设置与缓冲``vertices,uv``绑定在管线中传入``a_Position,a_UV``的值，最后再传入缓冲``indices``进行三角图元的绘制。









