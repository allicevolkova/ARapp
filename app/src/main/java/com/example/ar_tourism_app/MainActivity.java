package com.example.ar_tourism_app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

public class MainActivity extends AppCompatActivity implements
        FragmentOnAttachListener,
        BaseArFragment.OnSessionConfigurationListener {

    private final List<CompletableFuture<Void>> futures = new ArrayList<>();
    private ArFragment arFragment;
    private boolean rabbitDetected = false;

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

        // Images to be detected by our AR need to be added in AugmentedImageDatabase
        // This is how database is created at runtime
        // You can also prebuild database in you computer and load it directly (see: https://developers.google.com/ar/develop/java/augmented-images/guide#database)

        AugmentedImageDatabase database = new AugmentedImageDatabase(session);

        Bitmap rabbitImage = BitmapFactory.decodeResource(getResources(), R.drawable.rabbit);
        // Every image has to have its own unique String identifier
        database.addImage("rabbit", rabbitImage);

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
        if (rabbitDetected) {
            return;
        }

        if (augmentedImage.getTrackingState() == TrackingState.TRACKING
                && augmentedImage.getTrackingMethod() == AugmentedImage.TrackingMethod.FULL_TRACKING) {

            // Setting anchor to the center of Augmented Image
            AnchorNode anchorNode = new AnchorNode(augmentedImage.createAnchor(augmentedImage.getCenterPose()));

            // If rabbit model haven't been placed yet and detected image has String identifier of "rabbit"
            // This is also example of model loading and placing at runtime
            if (!rabbitDetected && augmentedImage.getName().equals("rabbit")) {
                rabbitDetected = true;
                Toast.makeText(this, "Rabbit tag detected", Toast.LENGTH_LONG).show();

                anchorNode.setWorldScale(new Vector3(3.5f, 3.5f, 3.5f));
                arFragment.getArSceneView().getScene().addChild(anchorNode);

                futures.add(ModelRenderable.builder()
                        .setSource(this, Uri.parse("models/Rabbit.glb"))
                        .setIsFilamentGltf(true)
                        .build()
                        .thenAccept(rabbitModel -> {
                            TransformableNode modelNode = new TransformableNode(arFragment.getTransformationSystem());
                            modelNode.setRenderable(rabbitModel);
                            anchorNode.addChild(modelNode);
                        })
                        .exceptionally(
                                throwable -> {
                                    Toast.makeText(this, "Unable to load rabbit model", Toast.LENGTH_LONG).show();
                                    return null;
                                }));
            }
        }
        if (rabbitDetected) {
            arFragment.getInstructionsController().setEnabled(
                    InstructionsController.TYPE_AUGMENTED_IMAGE_SCAN, false);
        }
    }
}