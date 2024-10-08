package org.firstinspires.ftc.teamcode.updatedDrive3.objects;

import static org.firstinspires.ftc.teamcode.drive.OldDriveConstants.MOTOR_VELO_PID;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.ACCEL_CONSTRAINT;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.HEADING_PID;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.LATERAL_MULTIPLIER;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.MAX_ACCEL;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.MAX_ANG_ACCEL;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.MAX_ANG_VEL;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.OMEGA_WEIGHT;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.TRACK_WIDTH;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.TRANSLATIONAL_PID;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.VEL_CONSTRAINT;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.VX_WEIGHT;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.VY_WEIGHT;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.encoderTicksToInches;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.kA;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.kStatic;
import static org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants.kV;

import androidx.annotation.NonNull;

import com.acmerobotics.roadrunner.drive.DriveSignal;
import com.acmerobotics.roadrunner.drive.MecanumDrive;
import com.acmerobotics.roadrunner.followers.HolonomicPIDVAFollower;
import com.acmerobotics.roadrunner.followers.TrajectoryFollower;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.acmerobotics.roadrunner.trajectory.TrajectoryBuilder;
import com.acmerobotics.roadrunner.trajectory.constraints.AngularVelocityConstraint;
import com.acmerobotics.roadrunner.trajectory.constraints.MecanumVelocityConstraint;
import com.acmerobotics.roadrunner.trajectory.constraints.MinVelocityConstraint;
import com.acmerobotics.roadrunner.trajectory.constraints.ProfileAccelerationConstraint;
import com.acmerobotics.roadrunner.trajectory.constraints.TrajectoryAccelerationConstraint;
import com.acmerobotics.roadrunner.trajectory.constraints.TrajectoryVelocityConstraint;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequence;
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequenceBuilder;
import org.firstinspires.ftc.teamcode.updatedDrive3.constants.Constants;
import org.firstinspires.ftc.teamcode.updatedDrive3.main.Robot;
import org.firstinspires.ftc.teamcode.trajectorysequence.TrajectorySequenceRunnerUpdated;
import org.firstinspires.ftc.teamcode.util.LynxModuleUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Class that contains the drivetrain motors
 * and related RoadRunner functions
 * as well as the movement functions
 */


public class Drivetrain extends MecanumDrive {
    public DcMotorEx leftFront, leftRear, rightRear, rightFront;

    private List<DcMotorEx> motors;

    private TrajectorySequenceRunnerUpdated trajectorySequenceRunner;
    private TrajectoryFollower follower;

    private VoltageSensor batteryVoltageSensor;

    private List<Integer> lastEncPositions = new ArrayList<>();
    private List<Integer> lastEncVels = new ArrayList<>();

    private boolean isSimpleRotating = false, isSimpleStraight = false, isSmoothBraking = false;

    ElapsedTime timer = new ElapsedTime();

    public Trajectory currentTrajectory;

    private Robot robot;


    public Drivetrain(HardwareMap hardwareMap, Robot rob) {
        super(kV, kA, kStatic, TRACK_WIDTH, TRACK_WIDTH, LATERAL_MULTIPLIER);

        robot = rob;

        leftFront = hardwareMap.get(DcMotorEx.class, "leftFront");
        leftRear = hardwareMap.get(DcMotorEx.class, "leftRear");
        rightFront = hardwareMap.get(DcMotorEx.class, "rightFront");
        rightRear = hardwareMap.get(DcMotorEx.class, "rightRear");

        motors = Arrays.asList(leftFront, leftRear, rightFront, rightRear);

        for (DcMotorEx motor : motors) {
            motor.setMode(DcMotorEx.RunMode.STOP_AND_RESET_ENCODER);

            motor.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);

            motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        leftFront.setDirection(DcMotorEx.Direction.REVERSE);
        leftRear.setDirection(DcMotorSimple.Direction.REVERSE);
        rightFront.setDirection(DcMotorSimple.Direction.FORWARD);
        rightRear.setDirection(DcMotorSimple.Direction.FORWARD);

        follower = new HolonomicPIDVAFollower(TRANSLATIONAL_PID, TRANSLATIONAL_PID, HEADING_PID,
                new Pose2d(0.5, 0.5, Math.toRadians(5.0)), 0.5);

        LynxModuleUtil.ensureMinimumFirmwareVersion(hardwareMap);

        batteryVoltageSensor = hardwareMap.voltageSensor.iterator().next();

        if (Constants.RUN_USING_ENCODER && MOTOR_VELO_PID != null) {
            setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, MOTOR_VELO_PID);
        }

        List<Integer> lastTrackingEncPositions = new ArrayList<>();
        List<Integer> lastTrackingEncVels = new ArrayList<>();

        trajectorySequenceRunner = new TrajectorySequenceRunnerUpdated(
                follower, HEADING_PID, batteryVoltageSensor,
                lastEncPositions, lastEncVels, lastTrackingEncPositions, lastTrackingEncVels
        );

    }

    /*** Roadrunner functions */

    public TrajectoryBuilder trajectoryBuilder(Pose2d startPose) {
        return new TrajectoryBuilder(startPose, VEL_CONSTRAINT, ACCEL_CONSTRAINT);
    }

    public TrajectoryBuilder trajectoryBuilder(Pose2d startPose, boolean reversed) {
        return new TrajectoryBuilder(startPose, reversed, VEL_CONSTRAINT, ACCEL_CONSTRAINT);
    }

    public TrajectoryBuilder trajectoryBuilder(Pose2d startPose, double startHeading) {
        return new TrajectoryBuilder(startPose, startHeading, VEL_CONSTRAINT, ACCEL_CONSTRAINT);
    }

    public TrajectorySequenceBuilder trajectorySequenceBuilder(Pose2d startPose) {
        return new TrajectorySequenceBuilder(
                startPose,
                VEL_CONSTRAINT, ACCEL_CONSTRAINT,
                MAX_ANG_VEL, MAX_ANG_ACCEL
        );
    }

    public void turnAsync(double angle) {
        trajectorySequenceRunner.followTrajectorySequenceAsync(
                trajectorySequenceBuilder(getPoseEstimate())
                        .turn(angle)
                        .build()
        );
    }

    public void turn(double angle) {
        turnAsync(angle);
        waitForIdle();
    }

    public void followTrajectoryAsync(Trajectory trajectory) {
        trajectorySequenceRunner.followTrajectorySequenceAsync(
                trajectorySequenceBuilder(trajectory.start())
                        .addTrajectory(trajectory)
                        .build()
        );
    }

    public void followTrajectory(Trajectory trajectory) {
        followTrajectoryAsync(trajectory);
        waitForIdle();
    }

    public void followTrajectorySequenceAsync(TrajectorySequence trajectorySequence) {
        trajectorySequenceRunner.followTrajectorySequenceAsync(trajectorySequence);
    }

    public void followTrajectorySequence(TrajectorySequence trajectorySequence) {
        followTrajectorySequenceAsync(trajectorySequence);
        waitForIdle();
    }

    public void breakFollowingSmooth() {
        setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        isSmoothBraking = true;
        timer.reset();

        trajectorySequenceRunner.breakFollowing();
    }

    public void breakFollowingImmediately() {
        trajectorySequenceRunner.breakFollowing();
    }

    public Pose2d getLastError() {
        return trajectorySequenceRunner.getLastPoseError();
    }

    public void update() {
        if (isSmoothBraking && timer.seconds() > 0.5) {
            isSmoothBraking = false;
            setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        updatePoseEstimate();
        DriveSignal signal = trajectorySequenceRunner.update(getPoseEstimate(), getPoseVelocity());
        if (signal != null) setDriveSignal(signal);
    }

    public void waitForIdle() {
        while (!Thread.currentThread().isInterrupted() && isBusy())
            update();
    }

    public boolean isBusy() {
        return trajectorySequenceRunner.isBusy();
    }

    public void setMode(DcMotor.RunMode runMode) {
        for (DcMotorEx motor : motors) {
            motor.setMode(runMode);
        }
    }

    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior zeroPowerBehavior) {
        for (DcMotorEx motor : motors) {
            motor.setZeroPowerBehavior(zeroPowerBehavior);
        }
    }

    public void setPIDFCoefficients(DcMotor.RunMode runMode, PIDFCoefficients coefficients) {
        PIDFCoefficients compensatedCoefficients = new PIDFCoefficients(
                coefficients.p, coefficients.i, coefficients.d,
                coefficients.f * 12 / batteryVoltageSensor.getVoltage()
        );

        for (DcMotorEx motor : motors) {
            motor.setPIDFCoefficients(runMode, compensatedCoefficients);
        }
    }

    public void setWeightedDrivePower(Pose2d drivePower) {
        Pose2d vel = drivePower;

        if (Math.abs(drivePower.getX()) + Math.abs(drivePower.getY())
                + Math.abs(drivePower.getHeading()) > 1) {
            // re-normalize the powers according to the weights
            double denom = VX_WEIGHT * Math.abs(drivePower.getX())
                    + VY_WEIGHT * Math.abs(drivePower.getY())
                    + OMEGA_WEIGHT * Math.abs(drivePower.getHeading());

            vel = new Pose2d(
                    VX_WEIGHT * drivePower.getX(),
                    VY_WEIGHT * drivePower.getY(),
                    OMEGA_WEIGHT * drivePower.getHeading()
            ).div(denom);
        }

        setDrivePower(vel);

    }

    @NonNull
    @Override
    public List<Double> getWheelPositions() {
        lastEncPositions.clear();

        List<Double> wheelPositions = new ArrayList<>();
        for (DcMotorEx motor : motors) {
            int position = motor.getCurrentPosition();
            lastEncPositions.add(position);
            wheelPositions.add(encoderTicksToInches(position));
        }
        return wheelPositions;
    }

    @Override
    public List<Double> getWheelVelocities() {
        lastEncVels.clear();

        List<Double> wheelVelocities = new ArrayList<>();
        for (DcMotorEx motor : motors) {
            int vel = (int) motor.getVelocity();
            lastEncVels.add(vel);
            wheelVelocities.add(encoderTicksToInches(vel));
        }
        return wheelVelocities;
    }

    @Override
    public void setMotorPowers(double v, double v1, double v2, double v3) {
        leftFront.setPower(v);
        leftRear.setPower(v1);
        rightRear.setPower(v2);
        rightFront.setPower(v3);
    }

    //Use in the IMU, NOT HERE
    @Override
    public double getRawExternalHeading() {
        return robot.imu.getRawExternalHeading();
    }

    public static TrajectoryVelocityConstraint getVelocityConstraint(double maxVel, double maxAngularVel, double trackWidth) {
        return new MinVelocityConstraint(Arrays.asList(
                new AngularVelocityConstraint(maxAngularVel),
                new MecanumVelocityConstraint(maxVel, trackWidth)
        ));
    }

    public static TrajectoryAccelerationConstraint getAccelerationConstraint(double maxAccel) {
        return new ProfileAccelerationConstraint(maxAccel);
    }

    /*** Added functions */

    public boolean isSimpleRotating() {
        if (isSimpleRotating && isBusy() && !Thread.currentThread().isInterrupted()) {
            return true; //if currently busy and is supposed to be rotating

        } else if (!isBusy() && isSimpleRotating) {
            isSimpleRotating = false; //if it is not busy but says it is rotating
            return false;

        }

        return false;

    }

    public boolean isSimpleStraight() {
        if (isSimpleStraight && isBusy() && !Thread.currentThread().isInterrupted()) {
            return true; //if currently busy and is supposed to be going straight

        } else if (!isBusy() && isSimpleStraight) {
            isSimpleStraight = false; //if it is not busy but says it is going straight
            return false;

        }

        return false;

    }

    public void simpleRotate(double angle) {
        isSimpleRotating = true;
        turnAsync(Math.toRadians(angle));

    }

    public void simpleRotate(double angle, double maxVel) {
        isSimpleRotating = true;
        trajectorySequenceRunner.followTrajectorySequenceAsync(
                trajectorySequenceBuilder(getPoseEstimate())
                        .turn(Math.toRadians(angle), maxVel, 1.0)
                        .build()
        );

    }

    public void simpleStraight(double distance) {
        isSimpleStraight = true;

        Trajectory goForward = trajectoryBuilder(
                getPoseEstimate())
                .forward(distance)
                .build();

        followTrajectoryAsync(goForward);

    }

    /*public boolean isSimpleStraightMinusEnd() {
        return false;
    }

    public boolean isSimpleStraightJustEnd() {
        return false;
    }*/
}
