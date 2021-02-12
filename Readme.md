⠀



¿Postprocesar videos o quieres reducir el tamaño del video antes de subirlo? Eche un vistazo a nuestro transcodificador .

¿Le gusta el proyecto, sacar provecho de él o simplemente quiere agradecerle? ¡Considere patrocinarme o donar !

¿Necesita soporte, consultoría o tiene alguna otra pregunta relacionada con el negocio? No dude en ponerse en contacto .

Vista de cámara
CameraView es una biblioteca de alto nivel bien documentada que facilita la captura de imágenes y videos, aborda la mayoría de los problemas y necesidades comunes, y aún lo deja con flexibilidad donde sea necesario.

api ' com.otaliastudios: cameraview: 2.7.0 '
Rápido y confiable
Soporte de gestos [docs]
Filtros en tiempo real [documentos]
Motor impulsado por Camera1 o Camera2 [docs]
Soporte de procesamiento de fotogramas [docs]
Marcas de agua y superposiciones animadas [docs]
Vista previa con tecnología OpenGL [docs]
Toma contenido de alta calidad con takePicturey takeVideo [docs]
Toma instantáneas súper rápidas con takePictureSnapshoty takeVideoSnapshot [docs]
CameraViewTamaño inteligente: cree un tamaño de cualquier tamaño [docs]
Controla HDR, flash, zoom, balance de blancos, exposición, ubicación, dibujo de cuadrícula y más [docs]
Soporte de imágenes RAW [docs]
Ligero
Funciona hasta el nivel de API 15
Bien probado
⠀



⠀

Apoyo
Si te gusta el proyecto, obtienes beneficios de él o simplemente quieres agradecer, ¡considera patrocinarme a través del programa de patrocinadores de GitHub! Puede tener el logotipo de su empresa aquí, obtener horas de soporte privado o simplemente ayudarme a impulsar esto. Si lo prefiere, también puede donar a nuestra página OpenCollective.

CameraView cuenta con el respaldo de ShareChat , una aplicación de redes sociales con más de 100 millones de descargas.



No dude en contactarme para soporte, consultoría o cualquier otra pregunta relacionada con el negocio.

Gracias a todos los patrocinadores de nuestro proyecto ... [conviértete en patrocinador]



... ¡ya todos nuestros patrocinadores de proyectos! [conviértete en patrocinador]

         

Preparar
Lea el sitio web oficial para obtener instrucciones de configuración y documentación. También puede estar interesado en nuestro registro de cambios o en la guía de migración v1 . Usar CameraView es extremadamente simple:

< com .otaliastudios.cameraview.CameraView
     xmlns : app = " http://schemas.android.com/apk/res-auto "
     android : layout_width = " wrap_content "
     android : layout_height = " wrap_content "
     aplicación : cameraPictureSizeMinWidth = " @integer / picture_min_width "
     aplicación : cameraPictureSizeMinHeight = " @ integer / picture_min_height "
     aplicación : cameraPictureSizeMaxWidth= " @ Entero / picture_max_width "
     aplicación : cameraPictureSizeMaxHeight = " @ entero / picture_max_height "
     aplicación : cameraPictureSizeMinArea = " @ entero / picture_min_area "
     aplicación : cameraPictureSizeMaxArea = " @ entero / picture_max_area "
     aplicación : cameraPictureSizeSmallest = " falsa | verdadera "
     aplicación : cameraPictureSizeBiggest = " falsa | verdadera "
     aplicación :cameraPictureSizeAspectRatio = " @ string / video_ratio "
     aplicación : cameraVideoSizeMinWidth = " @ número entero / video_min_width "
     aplicación : cameraVideoSizeMinHeight = " @ número entero / video_min_height "
     aplicación : cameraVideoSizeMaxWidth = " @ número entero / video_max_width "
     aplicación : cameraVideoSizeMaxHeight = " @ número entero / video_max_height "
     aplicación : cameraVideoSizeMinArea = " @ integer / video_min_area "
    aplicación: CameraVideoSizeMaxArea = " @ entero / video_max_area "
     aplicación : cameraVideoSizeSmallest = " falsa | verdadera "
     aplicación : cameraVideoSizeBiggest = " falsa | verdadera "
     aplicación : cameraVideoSizeAspectRatio = " @ string / video_ratio "
     aplicación : cameraSnapshotMaxWidth = " @ entero / snapshot_max_width "
     aplicación : cameraSnapshotMaxHeight = " @ número entero / snapshot_max_height "
     aplicación :cameraFrameProcessingMaxWidth = " @ número entero / processing_max_width "
     aplicación : cameraFrameProcessingMaxHeight = " @ número entero / processing_max_height "
     aplicación : cameraFrameProcessingFormat = " @ número entero / processing_format "
     aplicación : cameraFrameProcessingPoolSize = " @ número entero / processing_pool_size "
     app : cameraFrameProcessingExecutors = " @ número entero / processing_executors "
     aplicación : cameraVideoBitRate = "@ integer / video_bit_rate "
     app : cameraAudioBitRate = " @ integer / audio_bit_rate "
     app : cameraGestureTap = " none | autoFocus | takePicture "
     aplicación : cameraGestureLongTap = " none | autoFocus | takePicture "
     aplicación : cameraGesturePinch = " ninguno | zoom | exposiciónCorrección | filter filterControl2 "
     aplicación : cameraGestureScrollHorizontal = " none | zoom | exposiciónCorrection | filterControl1 | filterControl2 "
     aplicación: CameraGestureScrollVertical = " none | zoom | exposureCorrection | filterControl1 | filterControl2 "
     aplicación : cameraEngine = " camera1 | camera2 "
     aplicación : cameraPreview = " glSurface | superficie | textura "
     aplicación : cameraPreviewFrameRate = " @ entero / preview_frame_rate "
     aplicación : cameraPreviewFrameRateExact = " falsa | verdadero "
     app : cameraFacing = " back | front "
    aplicación :cameraHdr = " on | off "
     aplicación : cameraFlash = " on | auto | antorcha | off "
     aplicación : cameraWhiteBalance = " auto | nublado | luz del día | fluorescente | incandescente "
     aplicación : cameraMode = " imagen | video "
     aplicación : cameraAudio = " on | off | mono | stereo "
     aplicación : cameraGrid = " draw3x3 | draw4x4 | drawPhi | off "
     aplicación : cameraGridColor = "@ color / grid_color "
     app : cameraPlaySounds = " true | false "
     app : cameraVideoMaxSize = " @ entero / video_max_size "
     aplicación : cameraVideoMaxDuration = " @ entero / video_max_duration "
     aplicación : cameraVideoCodec = " deviceDefault | H264 | h263 "
     aplicación : cameraAutoFocusResetDelay = " @ integer / autofocus_delay "
     aplicación : cameraAutoFocusMarker = "@ string / cameraview_default_autofocus_marker "
     app : cameraUseDeviceOrientation = " true | false "
     app : cameraFilter = " @ string / real_time_filter "
     app : cameraPictureMetering = " true | false "
     app : cameraPictureSnapshotMetering = " false | true "
     app : cameraPictureFormat = " jpeg | "
     app : cameraRequestPermissions = " true | false"
     app : cameraExperimental = " false | true " >
    
    <! - Marca de agua! -> 
    < ImageView 
        android : layout_width = " wrap_content "
         android : layout_height = " wrap_content "
         android : layout_gravity = " bottom | end "
         android : src = " @ drawable / watermark "
         app : layout_drawOnPreview = " true | false "
         aplicación : layout_drawOnPictureSnapshot = " verdadero | falso"
         aplicación : layout_drawOnVideoSnapshot = " true | false " />
        
</ com .otaliastudios.cameraview.CameraVi
