# WheelProgress
##Download
You can Download 'WheelProgress' by adding in your build.gradle dependencies:
####dependencies {<br>&emsp;'com.github.PatelMilan/WheelProgress:wheelprogress:1.0.0'<br>}

##Usage
XML FIle

&lt;csiw.com.wheelprogress.WheelProgress<br>
&emsp;android:layout_width="match_parent"<br>
&emsp;android:layout_height="match_parent"<br>
&emsp;android:layout_gravity="center"<br>
&emsp;app:colorOfBar="@color/c_carrot"<br>
&emsp;app:colorOfRim="@color/c_asbestos"<br>
&emsp;app:progressIndeterminate="true"<br>
&emsp;app:progressOfLinear="false"<br>
&emsp;app:radiusOfCircle="120dp"<br>
&emsp;app:radiusOfFill="false"<br>
&emsp;app:speedOfSpin="25"<br>
&emsp;app:timeOfSpinBarCycle="250"<br>
&emsp;app:widthOfBar="5dp"<br>
&emsp;app:widthOfRim="5dp"/&gt;<br>
<br>
##OR<br>
<br>
##Java Code	<br>
<br>
WheelProgress wp = new WheelProgress(context);<br>
wp.setColorOfBar(Color.RED);<br>
