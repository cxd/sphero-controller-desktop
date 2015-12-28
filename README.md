__Sphero Robot Experiments__

This project contains experimentation with different methods of building robot controllers for the sphero device.

The aim will be to experiment with building robot controllers for computer controlled navigation
of the sphero robot.

The API it uses for communication is the bluecove API and a modified version of the 
[__nicklasgav Sphero-Desktop-API__](https://github.com/nicklasgav/Sphero-Desktop-API) 
which includes some additional changes for the sensor streaming api, the changes are located here in the [fork of Sphero-Desktop-API](https://github.com/cxd/Sphero-Desktop-API.git).


A simple _RandomController_ is implemented to provide a simple random walk implementation.
 
Currently the _BinomialCollisionController_ extends upon the random controller to enable the use of a 
beta-binomial distribution in assigning the probability of collision to 1..360 degrees for the heading
of the robot. This likelihood is then used to select the heading based on those headings least likely
to result in a collision.
This is little more than the random controller with a small modification to assign a likelihood to collision
detection.

