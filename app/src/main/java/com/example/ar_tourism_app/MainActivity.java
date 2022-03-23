package com.example.ar_tourism_app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Sceneform;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.BaseArFragment;
import com.google.ar.sceneform.ux.InstructionsController;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener {

    private final List<CompletableFuture<Void>> futures = new ArrayList<>();
    private ArFragment arFragment;
    private boolean doorDetected = false;
    private boolean churchDetected = false;
    private boolean lighthouseDetected = false;
    private boolean museumDetected = false;
    //


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getSupportFragmentManager().addFragmentOnAttachListener(this);

        if (savedInstanceState == null) {
            if (Sceneform.isSupported(this)) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.arFragment, ArFragment.class, null)
                        .commit();
            }
        }
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
        if (fragment.getId() == R.id.arFragment) {
            arFragment = (ArFragment) fragment;
            arFragment.setOnSessionConfigurationListener(this);
        }
    }

    @Override
    public void onSessionConfiguration(Session session, Config config) {
        // Disable plane detection
        config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED);
        config.setFocusMode(Config.FocusMode.AUTO);
        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)

        AugmentedImageDatabase database = new AugmentedImageDatabase(session);

        Bitmap doorImage = BitmapFactory.decodeResource(getResources(), R.drawable.door);
        // Every image has to have its own unique String identifier
        database.addImage("door", doorImage);

        Bitmap churchImage = BitmapFactory.decodeResource(getResources(), R.drawable.church);
        database.addImage("church", churchImage);

        Bitmap lighthouseImage = BitmapFactory.decodeResource(getResources(), R.drawable.lighthouse);
        database.addImage("lighthouse", lighthouseImage);

        Bitmap museumImage = BitmapFactory.decodeResource(getResources(), R.drawable.museum);
        database.addImage("museum", museumImage);

        config.setAugmentedImageDatabase(database);

        // Check for image detection
        arFragment.setOnAugmentedImageUpdateListener(this::onAugmentedImageTrackingUpdate);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        futures.forEach(future -> {
            if (!future.isDone())
                future.cancel(true);
        });
    }

    public void onAugmentedImageTrackingUpdate(AugmentedImage augmentedImage) {
        // If there are both images already detected, for better CPU usage we do not need scan for them
        if (doorDetected || churchDetected || lighthouseDetected || museumDetected) {
            return;
        }

        if (augmentedImage.getTrackingState() == TrackingState.TRACKING
                && augmentedImage.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {

            // Setting anchor to the center of Augmented Image
            AnchorNode anchorNode = new AnchorNode(augmentedImage.createAnchor(augmentedImage.getCenterPose()));

            // If door model haven't been placed yet and detected image has String identifier of "door"
            // This is also example of model loading and placing at runtime
            if (!doorDetected && augmentedImage.getName().equals("door")) {
                doorDetected = true;
                Toast.makeText(this, "Door tag detected", Toast.LENGTH_LONG).show();

                anchorNode.setWorldScale(new Vector3(3.5f, 3.5f, 3.5f));
                arFragment.getArSceneView().getScene().addChild(anchorNode);

                futures.add(ModelRenderable.builder()
                        .setSource(this, Uri.parse("models/door.glb"))
                        .setIsFilamentGltf(true)
                        .build()
                        .thenAccept(doorModel->{
                            TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                            modelNode.getScaleController().setMaxScale(0.003f);
                            modelNode.getScaleController().setMinScale(0.002f);
                            modelNode.setRenderable(doorModel);
                            anchorNode.addChild(modelNode);
                        })
                        .exceptionally(
                                throwable -> {
                                    Toast.makeText(this, "Unable to load door model", Toast.LENGTH_LONG).show();
                                    return null;
                                }));
            }
            if (!churchDetected && augmentedImage.getName().equals("church")) {
                churchDetected = true;
                Toast.makeText(this, "Church tag detected", Toast.LENGTH_LONG).show();

                anchorNode.setWorldScale(new Vector3(3.5f, 3.5f, 3.5f));
                arFragment.getArSceneView().getScene().addChild(anchorNode);

                futures.add(ModelRenderable.builder()
                        .setSource(this, Uri.parse("models/church.glb"))
                        .setIsFilamentGltf(true)
                        .build()
                        .thenAccept(churchModel->{
                            TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                            modelNode.getScaleController().setMaxScale(0.003f);
                            modelNode.getScaleController().setMinScale(0.002f);
                            modelNode.setRenderable(churchModel);
                            anchorNode.addChild(modelNode);
                        })
                        .exceptionally(
                                throwable -> {
                                    Toast.makeText(this, "Unable to load church model", Toast.LENGTH_LONG).show();
                                    return null;
                                }));
            }
            if (!lighthouseDetected && augmentedImage.getName().equals("lighthouse")) {
                lighthouseDetected = true;
                Toast.makeText(this, "Lighthouse tag detected", Toast.LENGTH_LONG).show();

                anchorNode.setWorldScale(new Vector3(3.5f, 3.5f, 3.5f));
                arFragment.getArSceneView().getScene().addChild(anchorNode);

                futures.add(ModelRenderable.builder()
                        .setSource(this, Uri.parse("models/lighthouse.glb"))
                        .setIsFilamentGltf(true)
                        .build()
                        .thenAccept(lighthouseModel->{
                            TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                            modelNode.getScaleController().setMaxScale(0.006f);
                            modelNode.getScaleController().setMinScale(0.005f);
                            modelNode.setRenderable(lighthouseModel);
                            anchorNode.addChild(modelNode);
                        })
                        .exceptionally(
                                throwable -> {
                                    Toast.makeText(this, "Unable to load lighthouse model", Toast.LENGTH_LONG).show();
                                    return null;
                                }));
            }
            if (!museumDetected && augmentedImage.getName().equals("museum")) {
                museumDetected = true;
                Toast.makeText(this, "Museum tag detected", Toast.LENGTH_LONG).show();

                anchorNode.setWorldScale(new Vector3(3.5f, 3.5f, 3.5f));
                arFragment.getArSceneView().getScene().addChild(anchorNode);

                futures.add(ModelRenderable.builder()
                        .setSource(this, Uri.parse("models/museum.glb"))
                        .setIsFilamentGltf(true)
                        .build()
                        .thenAccept(museumModel->{
                            TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                            modelNode.getScaleController().setMaxScale(0.003f);
                            modelNode.getScaleController().setMinScale(0.002f);
                            modelNode.setRenderable(museumModel);
                            anchorNode.addChild(modelNode);
                        })
                        .exceptionally(
                                throwable -> {
                                    Toast.makeText(this, "Unable to load museum model", Toast.LENGTH_LONG).show();
                                    return null;
                                }));
            }
        }

        if (doorDetected || churchDetected || lighthouseDetected || museumDetected) {
            arFragment.getInstructionsController().setEnabled(
                    InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false);
        }
    }
}