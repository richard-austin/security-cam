import security.cam.Cameras
import security.cam.UserPasswordEncoderListener

// Place your Spring DSL code here
beans = {
    userPasswordEncoderListener(UserPasswordEncoderListener)
    camerasBean(Cameras)
}
