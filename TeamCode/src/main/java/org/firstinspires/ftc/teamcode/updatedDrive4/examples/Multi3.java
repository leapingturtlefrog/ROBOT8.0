package org.firstinspires.ftc.teamcode.updatedDrive4.examples;

import static org.firstinspires.ftc.teamcode.updatedDrive4.constants.Constants.TFOD_MODEL_FILE;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.tfod.TfodProcessor;

@TeleOp
public class Multi3 extends LinearOpMode
{
    @Override
    public void runOpMode() throws InterruptedException
    {
        TfodProcessor myTfodProcessor, myTfodProcessor2;

        TfodProcessor.Builder myTfodProcessorBuilder = new TfodProcessor.Builder();

        myTfodProcessorBuilder.setUseObjectTracker(false);
        myTfodProcessorBuilder.setMaxNumRecognitions(1);
        myTfodProcessorBuilder.setNumDetectorThreads(1);
        myTfodProcessorBuilder.setNumExecutorThreads(1);

        myTfodProcessor = myTfodProcessorBuilder.setModelFileName(TFOD_MODEL_FILE)
                    .setModelLabels(new String[]{""})
                    .build();

        myTfodProcessor.setMinResultConfidence((float) 0.70);

        myTfodProcessor2 = myTfodProcessorBuilder.setModelFileName(TFOD_MODEL_FILE)
                .setModelLabels(new String[]{""})
                .build();

        myTfodProcessor2.setMinResultConfidence((float) 0.70);

        //

        telemetry.setMsTransmissionInterval(50);

        int[] viewIDs = VisionPortal.makeMultiPortalView(2, VisionPortal.MultiPortalLayout.HORIZONTAL);

        VisionPortal portal1 = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .addProcessor(myTfodProcessor)
                .setLiveViewContainerId(viewIDs[0])
                .build();

        while (!isStopRequested() && portal1.getCameraState() != VisionPortal.CameraState.STREAMING)
        {
            telemetry.addLine("Waiting for portal 1 to come online");
            telemetry.update();
        }


        //portal1.stopStreaming();
        //portal1.stopLiveView();

        //sleep(500);

        if (isStopRequested())
        {
            return;
        }

        VisionPortal portal2 = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 2"))
                .addProcessor(myTfodProcessor2)
                .setLiveViewContainerId(viewIDs[1])
                .build();

        while (!isStopRequested() && portal2.getCameraState() != VisionPortal.CameraState.STREAMING)
        {
            telemetry.addLine("Waiting for portal 2 to come online");
            telemetry.update();
        }


        //portal1.stopStreaming();
        //portal1.stopLiveView();

        //sleep(500);

        if (isStopRequested())
        {
            return;
        }

        while (!isStopRequested())
        {
            telemetry.addLine("All cameras online");
            telemetry.update();
            sleep(500);
        }
    }
}