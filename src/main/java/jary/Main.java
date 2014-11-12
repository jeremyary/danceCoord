package jary;

import SimpleOpenNI.SimpleOpenNI;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Arrays;
import java.util.HashMap;

public class Main extends PApplet {

    ThreadPoolTaskExecutor taskExecutor;

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

        ApplicationContext context = new ClassPathXmlApplicationContext("spring.xml");
        taskExecutor = (ThreadPoolTaskExecutor) context.getBean("taskExecutor");

        lastImageTaken = System.currentTimeMillis();

        poolConfig = new JedisPoolConfig();
        jedisPool = new JedisPool(poolConfig, "172.31.253.53", 6379, 0);
//        jedisPool = new JedisPool(poolConfig, "localhost", 6379, 0);
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

        long currentTime = System.currentTimeMillis();

        // publish skeleton joints info
        taskExecutor.execute(new PublishRunnable(userId, currentTime));

        // publish heat map image
//        if (currentTime - lastImageTaken >= 5000) {
//            taskExecutor.execute(new PublishImageRunnable(currentTime));
//        }
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

    protected class PublishRunnable implements Runnable {

        protected Integer userId;
        protected HashMap<String, PVector> skelVectors = new HashMap<>();
        protected Long currentTime;

        public PublishRunnable(Integer userId, Long currentTime) {

            this.userId = userId;
            this.currentTime = currentTime;

            skelVectors.put("head", new PVector());
            skelVectors.put("neck", new PVector());
            skelVectors.put("left_shoulder", new PVector());
            skelVectors.put("left_elbow", new PVector());
            skelVectors.put("left_hand", new PVector());
            skelVectors.put("right_shoulder", new PVector());
            skelVectors.put("right_elbow", new PVector());
            skelVectors.put("right_hand", new PVector());
            skelVectors.put("torso", new PVector());
            skelVectors.put("left_hip", new PVector());
            skelVectors.put("left_knee", new PVector());
            skelVectors.put("left_foot", new PVector());
            skelVectors.put("right_hip", new PVector());
            skelVectors.put("right_knee", new PVector());
            skelVectors.put("right_foot", new PVector());
        }

        @Override
        public void run() {

            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_HEAD, skelVectors.get("head"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_NECK, skelVectors.get("neck"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_SHOULDER, skelVectors.get("left_shoulder"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_ELBOW, skelVectors.get("left_elbow"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_HAND, skelVectors.get("left_hand"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_SHOULDER, skelVectors.get("right_shoulder"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_ELBOW, skelVectors.get("right_elbow"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_HAND, skelVectors.get("right_hand"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_TORSO, skelVectors.get("torso"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_HIP, skelVectors.get("left_hip"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_KNEE, skelVectors.get("left_knee"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_LEFT_FOOT, skelVectors.get("left_foot"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_HIP, skelVectors.get("right_hip"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_KNEE, skelVectors.get("right_knee"));
            kinect.getJointPositionSkeleton(userId, SimpleOpenNI.SKEL_RIGHT_FOOT, skelVectors.get("right_foot"));

            String payload = "{ \"timestamp\": " + currentTime + ", " +
                    "\"points\": { " +
                    "\"head\": " + skelVectors.get("head") + ", " +
                    "\"neck\": " + skelVectors.get("neck") + ", " +
                    "\"left_shoulder\": " + skelVectors.get("left_shoulder") + ", " +
                    "\"left_elbow\": " + skelVectors.get("left_elbow") + ", " +
                    "\"left_hand\": " + skelVectors.get("left_hand") + ", " +
                    "\"right_shoulder\": " + skelVectors.get("right_shoulder") + ", " +
                    "\"right_elbow\": " + skelVectors.get("right_elbow") + ", " +
                    "\"right_hand\": " + skelVectors.get("right_hand") + ", " +
                    "\"torso\": " + skelVectors.get("torso") + ", " +
                    "\"left_hip\": " + skelVectors.get("left_hip") + ", " +
                    "\"left_knee\": " + skelVectors.get("left_knee") + ", " +
                    "\"left_foot\": " + skelVectors.get("left_foot") + ", " +
                    "\"right_hip\": " + skelVectors.get("right_hip") + ", " +
                    "\"right_knee\": " + skelVectors.get("right_knee") + ", " +
                    "\"right_foot\": " + skelVectors.get("right_foot") +
                    "} }";

            publisher.publish("dancer-state", payload);
        }
    }

    protected class PublishImageRunnable implements Runnable {

        protected long currentTime;

        public PublishImageRunnable(long currentTime) {
            this.currentTime = currentTime;
        }

        @Override
        public void run() {
            publisher.publish("dancer-image", Arrays.toString(kinect.depthImage().pixels));
            lastImageTaken = currentTime;
        }
    }
}