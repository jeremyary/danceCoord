So, this code was done for a hackathon...this won't be pretty.

We had a little less than 48 hours to put together a project, this is part of the result. We decided to build a system that would take input from a MS Kinect, determine skeletal joint info, use that info to calculate how many beats-per-minute a dancer was performing, and then adjust the speed of the music playing to match that of the dancer.

In short, we made music "music to the dancer" instead of making the dancer "dance to the music".

We initially set out on OS X machines to find a way to interface with the Kinect. Doing so outside of Windows 7+ with the provided MS SDK turned out to be quite a challenge. We had hoped to find a way to do so with Python, but most available options we tried either failed to install properly or still required the native SDK underneath the covers. Ultimately, I was able to find a small Java class in the SimpleOpenNI documentation that seemed to indicate a possible solution, so I dug in.

The best resource I found for getting SimpleOpenNI going on OS X was here:

http://blog.nelga.com/setup-microsoft-kinect-on-mac-os-x-10-9-mavericks/

The prereq's that I already had going were xcode, command line tools, xquartz, cmake, mac ports, libtool, & libusb +universal.

The next step was to add OpenNI SDK for OS X, which was available via Mega thanks to the blog post referenced. Following, SensorKinect was installed as the interface between OpenNI and the Kinect. Lastly, NiTE was added as a middleware layer that aids in detection of various aspects of color, depth, heat, etc. 

After all of these tools were in place, I was able to add SimpleOpenNI, which was yet another layer of abstraction that allowed OpenNI/NiTE to interface with Processing, something I knew I could get at and utilize in Java via PApplets thanks to the scriplet I had found.

Once all was said and done, I needed to include Processing's core.jar and the SimpleOpenNI.jar in the libraries of the project and run the app natively. Any attempt to bundle the jars via maven or otherwise resulted in a fairly common issue with Java projects that try and "export" a Processing app - the app loses visibility of the SimpleOpenNI install. With that in mind, it was around 3AM Friday night, so we resolved to use my laptop to trigger the Kinect interface, not worry about bundling a distributable, and we moved on.

