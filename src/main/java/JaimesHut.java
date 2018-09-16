import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

import com.jme3.animation.AnimControl;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioContext;
import com.jme3.audio.AudioData.DataType;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.audio.Listener;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.BulletAppState.ThreadingType;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.math.Vector3f;
import com.jme3.phonon.Phonon;
import com.jme3.phonon.PhononRenderer;
import com.jme3.phonon.PhononSettings;
import com.jme3.phonon.ThreadMode;
import com.jme3.phonon.desktop_javasound.JavaSoundPhononSettings;
import com.jme3.phonon.scene.PhononMesh;
import com.jme3.phonon.scene.PhononMeshBuilder;
import com.jme3.physicsloader.impl.bullet.BulletPhysicsLoader;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

import wf.frk.f3b.jme3.F3bKey;
import wf.frk.f3b.jme3.F3bLoader;
import wf.frk.f3b.jme3.runtime.F3bPhysicsRuntimeLoader;

/**
 * WARNING
 * This code has been written really fast with the purpose of testing the current status 
 * of jmePhonon on a real use case. If you need a properly written example, please refer to the
 * non-unit tests on the main repository.
 */
public class JaimesHut extends SimpleApplication implements PhysicsTickListener,ActionListener{
    public static void main(String[] args) {
        AppSettings settings=new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL3);
        USE_PHONON=true;

        JaimesHut app=new JaimesHut();
        app.setSettings(settings);
        app.setShowSettings(true);

        app.start();
    }

    static int OUTPUT_LINES=1;
    static int FRAME_SIZE=1024;// samples
    static int FRAME_BUFFER=3;
    static int MAX_PREBUFFERING=1024*2*4; //2 frame preload
    static int CHANNELS=2;
    BulletPhysicsLoader BULLET_PHYSICS_LOADER;
    BulletAppState PHYSICS;
    CharacterControl CHARACTER_CONTROL;
    int NB_LIGHTS=0;
    Vector3f WALKDIR=new Vector3f();
    Vector3f VIEWDIR=new Vector3f();

    WeakHashMap<Light,Spatial> LIGHTSxSPATIALS=new WeakHashMap<Light,Spatial>();

    AudioNode FOOTSTEPS,BACKGROUND;
    static boolean USE_PHONON=false;
    @Override
    public void simpleInitApp() {
      
        this.setPauseOnLostFocus(false);
        this.setDisplayStatView(false);
        this.setDisplayFps(true);
        if (USE_PHONON) {
                  try {
                JavaSoundPhononSettings settings=new JavaSoundPhononSettings();
                Phonon.init(settings, this);
            } catch (Exception e1) {
                e1.printStackTrace();
            }         
        }



        flyCam.setMoveSpeed(0);
        
        PHYSICS=new BulletAppState();
        PHYSICS.setThreadingType(ThreadingType.PARALLEL);


        
        stateManager.attach(PHYSICS);

        CapsuleCollisionShape capsule = new CapsuleCollisionShape(.6f, 1f);
        CHARACTER_CONTROL=new CharacterControl(capsule,0.01f);
        CHARACTER_CONTROL.setGravity(new Vector3f(0,-9.81f,0));
        CHARACTER_CONTROL.setPhysicsLocation(new Vector3f(0,10,0));
        PHYSICS.getPhysicsSpace().add(CHARACTER_CONTROL);
        PHYSICS.getPhysicsSpace().addTickListener(this);


        inputManager.addMapping("LEFT",new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("RIGHT",new KeyTrigger(KeyInput.KEY_D) );
        inputManager.addMapping("DOWN",new KeyTrigger(KeyInput.KEY_S) );
        inputManager.addMapping("UP",new KeyTrigger(KeyInput.KEY_W) );

        inputManager.addListener(this, "LEFT","RIGHT","UP","DOWN");


        F3bLoader.init(assetManager);

        BULLET_PHYSICS_LOADER=new BulletPhysicsLoader();
        BULLET_PHYSICS_LOADER.useCompoundCapsule(true);

     
        F3bKey key=new F3bKey("scene.f3b")
        .usePhysics(BULLET_PHYSICS_LOADER).useEnhancedRigidbodies(true)
        .useEnhancedGhostbodies(true);

        Spatial scene=assetManager.loadModel(key);

        rootNode.attachChild(scene);
        F3bPhysicsRuntimeLoader.load(key,scene,PHYSICS.getPhysicsSpace());
        
        // Attach all lights to rootnode
        scene.depthFirstTraversal(sx -> {
            for(Light l:sx.getLocalLightList()){
                LIGHTSxSPATIALS.put(l,sx);
                sx.removeLight(l);
                rootNode.addLight(l);
                NB_LIGHTS++;
            }
            if(sx instanceof AudioNode){
                AudioNode an=(AudioNode)sx;
                if(!USE_PHONON){
                    an.setRefDistance(1f);
                    an.setMaxDistance(10000);
                    an.setVolume(an.getVolume()*0.5f);
                    an.setVelocityFromTranslation(false);
                    an.setDryFilter(null);
                    an.setReverbEnabled(false);
                }
                an.play();
            }
            if(sx.getControl(AnimControl.class)!=null){
                AnimControl anim=sx.getControl(AnimControl.class);
             
                anim.createChannel().setAnim(anim.getAnimationNames().iterator().next());

                
            }
        });

        renderManager.setSinglePassLightBatchSize(NB_LIGHTS);

        FOOTSTEPS=new AudioNode(assetManager,"Sounds/267492__snumen__footsteps-on-forest-ground.wav",DataType.Buffer);
        FOOTSTEPS.setPitch(0.9f);
        rootNode.attachChild(FOOTSTEPS);
        FOOTSTEPS.setPositional(true);
        FOOTSTEPS.setLooping(true);
        FOOTSTEPS.setVolume(0.1f);

        BACKGROUND=new AudioNode(assetManager,"Sounds/423134__dkiller2204__forestmusic.wav",DataType.Buffer);
        BACKGROUND.setPositional(false);
        BACKGROUND.setVolume(0.3f);
        BACKGROUND.setLooping(true);
        BACKGROUND.play();

        PhononMesh mesh=PhononMeshBuilder.build(rootNode,(sx) -> {
            return (!(sx instanceof Geometry))||sx.getParent().getUserData("game.soundoccluder")!=null;

            
        });
        PhononRenderer renderer=(PhononRenderer)audioRenderer;
        renderer.setScene(mesh);
        renderer.saveSceneAsObj("/tmp/scene.obj");
    }
    boolean left = false, right = false, up = false, down = false;
    Vector3f camDir=new Vector3f();
    Vector3f camLeft=new Vector3f();
    @Override
    public void simpleUpdate(float tpf) {
        Iterator<Entry<Light,Spatial>> lights_i=LIGHTSxSPATIALS.entrySet().iterator();
        while(lights_i.hasNext()){
            Entry<Light,Spatial> entry=lights_i.next();
            Light l=entry.getKey();
            if(l instanceof  PointLight){
                PointLight pl=(PointLight)l;
                pl.setPosition(entry.getValue().getWorldTranslation());
            }else if(l instanceof  SpotLight){
                SpotLight pl=(SpotLight)l;
                pl.setPosition(entry.getValue().getWorldTranslation());
            }
        }

        cam.setLocation(CHARACTER_CONTROL.getPhysicsLocation().addLocal(0,0f,0));
        VIEWDIR.set(cam.getDirection());
        this.listener.setLocation(cam.getLocation());
        FOOTSTEPS.setLocalTranslation(cam.getLocation());

        camDir.set(cam.getDirection());
        camDir.multLocal(0.1f);
        camLeft.set(cam.getLeft());
        camLeft.multLocal(0.1f);
        camDir.y = 0;
        camLeft.y = 0;
        WALKDIR.set(0, 0, 0);
        if (left) {
            WALKDIR.addLocal(camLeft);
        }
        if(right){
            camLeft.negateLocal();
            WALKDIR.addLocal(camLeft);
        }
        if (up) {
            WALKDIR.addLocal(camDir);
        }
        if(down){
            camDir.negateLocal();
            WALKDIR.addLocal(camDir);
        }
     
        if(WALKDIR.length()>0){
            if(FOOTSTEPS.getStatus()!=AudioSource.Status.Playing)
            FOOTSTEPS.play();
        }
        else{
            if(FOOTSTEPS.getStatus()==AudioSource.Status.Playing)

            FOOTSTEPS.pause();
        }

    }

    @Override
    public void prePhysicsTick(PhysicsSpace space, float tpf) {
        CHARACTER_CONTROL.setWalkDirection(WALKDIR);
        CHARACTER_CONTROL.setViewDirection(VIEWDIR);

    }

    @Override
    public void physicsTick(PhysicsSpace space, float tpf) {
    
	}

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch(name){
            case "UP":
                up=isPressed;
                break;
            case "DOWN":
                down=isPressed;
                break;
            case "LEFT":
                left=isPressed;
                break;
            case "RIGHT":
                right=isPressed;
                break;
        }
    }

    
}