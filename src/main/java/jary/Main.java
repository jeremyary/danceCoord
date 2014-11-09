package jary;

import SimpleOpenNI.SimpleOpenNI;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

public class Main extends PApplet {

    // create kinect object
    SimpleOpenNI kinect;
    // image storage from kinect
    PImage kinectDepth;
    // int of each user being  tracked
    int[] userID;

    // postion of head to draw circle
    PVector headPosition = new PVector();
    // turn headPosition into scalar form
    float distanceScalar;
    // diameter of head drawn in pixels
    float headSize = 200;

    // threshold of level of confidence
    float confidenceLevel = Float.parseFloat("0.5");
    // the current confidence level that the kinect is tracking
    float confidence;
    // vector of tracked head for confidence checking
    PVector confidenceVector = new PVector();

    private CountDownLatch publishLatch = new CountDownLatch(1);

    protected JedisPoolConfig poolConfig;
    protected JedisPool jedisPool;
    protected Jedis publisher;

    protected long lastImageTaken;

    public static void main(String args[]) {

        PApplet.main(new String[]{"--present", "jary.Main"});
    }

    /*---------------------------------------------------------------
    Starts new kinect object and enables skeleton tracking.
    Draws window
    ----------------------------------------------------------------*/
    public void setup() {

        lastImageTaken = System.currentTimeMillis();

        poolConfig = new JedisPoolConfig();
//        jedisPool = new JedisPool(poolConfig, "172.31.253.53", 6379, 0);
        jedisPool = new JedisPool(poolConfig, "localhost", 6379, 0);
        publisher = jedisPool.getResource();

        System.out.println("Connection to server sucessfully");
        //check whether server is running or not
        System.out.println("Server is running: " + publisher.ping());

        // start a new kinect object
        kinect = new SimpleOpenNI(this);

        // enable depth sensor
        kinect.enableDepth();

        // enable skeleton generation for all joints
        kinect.enableUser();

        // draw thickness of drawer
        strokeWeight(3);
        // smooth out drawing
        smooth();

        // create a window the size of the depth information
        size(kinect.depthWidth(), kinect.depthHeight());
    } // void setup()

    /*---------------------------------------------------------------
    Updates Kinect. Gets users tracking and draws skeleton and
    head if confidence of tracking is above threshold
    ----------------------------------------------------------------*/
    public void draw() {
        // update the camera
        kinect.update();
        // get Kinect data
        kinectDepth = kinect.depthImage();
        // draw depth image at coordinates (0,0)
        image(kinectDepth, 0, 0);

        // get all user IDs of tracked users
        userID = kinect.getUsers();

        // loop through each user to see if tracking
        for (int i = 0; i < userID.length; i++) {
            // if Kinect is tracking certain user then get joint vectors
            if (kinect.isTrackingSkeleton(userID[i])) {
                // get confidence level that Kinect is tracking head
                confidence = kinect.getJointPositionSkeleton(userID[i],
                        SimpleOpenNI.SKEL_HEAD, confidenceVector);

                // if confidence of tracking is beyond threshold, then track user
                if (confidence > confidenceLevel) {
                    // change draw color based on hand id#
                    stroke(0, 0, 255);
                    // fill the ellipse with the same color
                    fill(0, 255, 0);
                    // draw the rest of the body
                    drawSkeleton(userID[i]);

                }
            }
        }
    }

    /*---------------------------------------------------------------
    Draw the skeleton of a tracked user.  Input is userID
    ----------------------------------------------------------------*/
    public void drawSkeleton(int userId) {

        // get 3D position of head
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_HEAD, headPosition);

        // convert real world point to projective space
        kinect.convertRealWorldToProjective(headPosition, headPosition);

        // create a distance scalar related to the depth in z dimension
        distanceScalar = (525 / headPosition.z);

        // draw the circle at the position of the head with the head size scaled by the distance scalar
        ellipse(headPosition.x, headPosition.y, distanceScalar * headSize, distanceScalar * headSize);


        HashMap<String, PVector> skelVectors = new HashMap<>();

        skelVectors.put("SKEL_HEAD", new PVector());
        skelVectors.put("SKEL_NECK", new PVector());
        skelVectors.put("SKEL_LEFT_SHOULDER", new PVector());
        skelVectors.put("SKEL_LEFT_ELBOW", new PVector());
        skelVectors.put("SKEL_LEFT_HAND", new PVector());
        skelVectors.put("SKEL_RIGHT_SHOULDER", new PVector());
        skelVectors.put("SKEL_RIGHT_ELBOW", new PVector());
        skelVectors.put("SKEL_RIGHT_HAND", new PVector());
        skelVectors.put("SKEL_TORSO", new PVector());
        skelVectors.put("SKEL_LEFT_HIP", new PVector());
        skelVectors.put("SKEL_LEFT_KNEE", new PVector());
        skelVectors.put("SKEL_LEFT_FOOT", new PVector());
        skelVectors.put("SKEL_RIGHT_HIP", new PVector());
        skelVectors.put("SKEL_RIGHT_KNEE", new PVector());
        skelVectors.put("SKEL_RIGHT_FOOT", new PVector());

        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_HEAD, skelVectors.get("SKEL_HEAD"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_NECK, skelVectors.get("SKEL_NECK"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, skelVectors.get("SKEL_LEFT_SHOULDER"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, skelVectors.get("SKEL_LEFT_ELBOW"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_HAND, skelVectors.get("SKEL_LEFT_HAND"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, skelVectors.get("SKEL_RIGHT_SHOULDER"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, skelVectors.get("SKEL_RIGHT_ELBOW"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_HAND, skelVectors.get("SKEL_RIGHT_HAND"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_TORSO, skelVectors.get("SKEL_TORSO"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_HIP, skelVectors.get("SKEL_LEFT_HIP"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_KNEE, skelVectors.get("SKEL_LEFT_KNEE"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_FOOT, skelVectors.get("SKEL_LEFT_FOOT"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_HIP, skelVectors.get("SKEL_RIGHT_HIP"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, skelVectors.get("SKEL_RIGHT_KNEE"));
        kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_FOOT, skelVectors.get("SKEL_RIGHT_FOOT"));

        long currentTime = System.currentTimeMillis();
        String payload = "{ \"timestamp\": " + currentTime + ", " +
                "\"points\": { " +
                "\"head\": " + skelVectors.get("SKEL_HEAD") + ", " +
                "\"neck\": " + skelVectors.get("SKEL_NECK") + ", " +
                "\"left_shoulder\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"left_elbow\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"left_hand\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"right_shoulder\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"right_elbow\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"right_hand\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"torso\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"left_hip\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"left_knee\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"left_foot\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"right_hip\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"right_knee\": " + skelVectors.get("SKEL_LEFT_SHOULDER") + ", " +
                "\"right_foot\": " + skelVectors.get("SKEL_LEFT_SHOULDER") +
                "} }";

        publisher.publish("dancer-state", payload);

        if (currentTime - lastImageTaken >= 5000) {
            publisher.publish("dancer-image", Arrays.toString(kinect.depthImage().pixels));
            lastImageTaken = currentTime;
        }

        kinect.drawLimb(userId, SimpleOpenNI.SKEL_HEAD, SimpleOpenNI.SKEL_NECK);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_LEFT_SHOULDER);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_LEFT_ELBOW);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, SimpleOpenNI.SKEL_LEFT_HAND);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_NECK, SimpleOpenNI.SKEL_RIGHT_SHOULDER);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_RIGHT_ELBOW);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, SimpleOpenNI.SKEL_RIGHT_HAND);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, SimpleOpenNI.SKEL_TORSO);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_LEFT_HIP);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_HIP, SimpleOpenNI.SKEL_LEFT_KNEE);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_LEFT_KNEE, SimpleOpenNI.SKEL_LEFT_FOOT);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_TORSO, SimpleOpenNI.SKEL_RIGHT_HIP);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_HIP, SimpleOpenNI.SKEL_RIGHT_KNEE);
        kinect.drawLimb(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, SimpleOpenNI.SKEL_RIGHT_FOOT);
    }

    /*---------------------------------------------------------------
    When a new user is found, print new user detected along with
    userID and start pose detection.  Input is userID
    ----------------------------------------------------------------*/
    public void onNewUser(SimpleOpenNI curContext, int userId) {
        println("New User Detected - userId: " + userId);
        // start tracking of user id
        curContext.startTrackingSkeleton(userId);
    }

    /*---------------------------------------------------------------
    Print when user is lost. Input is int userId of user lost
    ----------------------------------------------------------------*/
    public void onLostUser(SimpleOpenNI curContext, int userId) {
        // print user lost and user id
        println("User Lost - userId: " + userId);
    }

    /*---------------------------------------------------------------
    Called when a user is tracked.
    ----------------------------------------------------------------*/
    public void onVisibleUser(SimpleOpenNI curContext, int userId) {
    }


}