package com.example.proyecto_final_car_wash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ActivitySignUp : AppCompatActivity() {
    // Declarar variables de firebase.
    private lateinit var auth: FirebaseAuth;
    private lateinit var fireStore: FirebaseFirestore;

    // Declarar variables de los campos edit text.
    private lateinit var nombreEditText: TextInputEditText;
    private lateinit var apellidoEditText: TextInputEditText;
    private lateinit var telefonoEditText: TextInputEditText;
    private lateinit var correoEditText: TextInputEditText;
    private lateinit var passwordEditText: TextInputEditText;
    private lateinit var imageViewSelection: ImageView;

    // Variable para regresar a iniciar sesion.
    private lateinit var iniciarSesionTextView: TextView;

    // Declarar las variables de las opciones de la foto.
    private lateinit var tomarFotoTextView: TextView;
    private lateinit var seleccionarFotoTextView: TextView;

    // Declarar la variable para traer la clase de la camera
    // photo.
    private lateinit var classesCameraPhoto: ClassesCameraPhoto;

    // Declarar la variable para traer la clase del storage.
    private lateinit var classesStoragePhotos: ClassesStoragePhotos;

    // Declarar como vamos a tomar la foto utilizando
    // registerForActivityResult.
    private val tomarFoto =
    // Con registerForActivityResult() podemos manejar resultados
    // de imagenes.
    // Para el resultado se ejecuta cuando completamos
        // la actividad del metodo anterior.
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { resultado ->
            // Validamos el resultado si es verdadero o falso.
            if (resultado.resultCode === Activity.RESULT_OK) {
                // Declaramos la variable de la foto con el Uri.
                val photoUri = classesCameraPhoto.getPhotoUri();

                // Obtenemos dentro del contexto del objeto
                // photoUri.
                photoUri?.let {
                    // Declaramos una variable para obtener el
                    // bitmap.
                    val bitmap = classesCameraPhoto.getBitmapFromUri(it);

                    // Aqui establecemos donde se pondra la imagen
                    // cuando tengamos la decodificacion y el proceso
                    // anterior realizado.
                    imageViewSelection.setImageBitmap(bitmap);

                    // Le decimos que usaremos la imagen que tomamos
                    // en el classesStoragePhotos y seria como el contexto.
                    classesStoragePhotos.imageUri = it;
                }
            }
        }

    // Declarar variable del boton registrar.
    private lateinit var registerButton: AppCompatButton;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar firebase auth y store.
        auth = FirebaseAuth.getInstance();
        fireStore = FirebaseFirestore.getInstance();

        // Obtenemos el id de la foto
        imageViewSelection = findViewById(R.id.imageViewSelection);

        // Obtener los valores de los campos de textos.
        nombreEditText = findViewById(R.id.nombreEditText);
        apellidoEditText = findViewById(R.id.apellidoEditText);
        telefonoEditText = findViewById(R.id.telefonoEditText);
        correoEditText = findViewById(R.id.correoEditText);
        passwordEditText = findViewById(R.id.passwordEditText);

        // Obtenemos el valor de iniciar sesion para regresar.
        iniciarSesionTextView = findViewById(R.id.iniciarSesionTextView);

        // Obtener el valor del boton registrar.
        registerButton = findViewById(R.id.registerButton);

        // Inicializar la clase ClassesCameraPhoto.kt.
        classesCameraPhoto = ClassesCameraPhoto(this);

        // Inicializar la clase ClassesStoragePhotos.kt
        classesStoragePhotos = ClassesStoragePhotos(this, imageViewSelection);

        // Manejamos el click del image view.
        imageViewSelection.setOnClickListener {
            mostrarModalOpciones();
        }

        // Manejamos el click de regresar al inicio de sesion.
        iniciarSesionTextView.setOnClickListener {
            // Redirigimos a ActivitySignIn.
            val intent = Intent(this, ActivitySignIn::class.java);
            startActivity(intent);
        }

        // Cuando se haga click en el boton registrar.
        registerButton.setOnClickListener {
            // Convertir los campos de texto a
            // string.
            val nombre = nombreEditText.text.toString().trim();
            val apellido = apellidoEditText.text.toString().trim();
            val telefono = telefonoEditText.text.toString().trim();
            val correo = correoEditText.text.toString().trim();
            val password = passwordEditText.text.toString().trim();

            // Validar que los campos nos esten vacios.
            if (validateFields(nombre, apellido, telefono, correo, password)) {
                // Decimos que tambien queremos subir la imagen.
                classesStoragePhotos.subirImageFirebaseStorage(onSuccess = { imageUrl ->
                    // Llevar los datos a firebase auth y firebase store.
                    // Tambien decimos que si la imagen se pudo registrar
                    // la registramos.
                    registerUser(nombre, apellido, telefono, correo, password, imageUrl);
                }, onFailure = { exception ->
                    // Si hubiera algun error con la imagen entonces nos
                    // mostraria este toast.
                    Toast.makeText(
                        this, "Error al subir la imagen: ${exception.message}", Toast.LENGTH_SHORT
                    ).show()
                })
            }
        }
    }

    private fun mostrarModalOpciones() {
        val view = layoutInflater.inflate(R.layout.dialog_modal_photos_options, null);

        val dialog = AlertDialog.Builder(this).setView(view).create();

        // Obtener los id's de las variables.
        tomarFotoTextView = view.findViewById(R.id.tomarFotoTextView);
        seleccionarFotoTextView = view.findViewById(R.id.seleccionarFotoTextView);

        // Opcion para tomar foto
        tomarFotoTextView.setOnClickListener {
            // Revisamos si el usuario permitio usar
            // la camara.
            if (classesCameraPhoto.checkCameraPermission(this)) {
                // Declaramos la variable para traer la funcion
                // dispatch de la clase camera foto.
                val takePictureIntent = classesCameraPhoto.dispatchTakePictureIntent()

                // Aqui ejecutando el tomarFoto podremos
                // iniciar y tomar la foto.
                if (takePictureIntent != null) {
                    tomarFoto.launch(takePictureIntent)
                }
            }
            // Lo que haremos despues de darle click.
            dialog.dismiss();
        }

        // Opcion para tomar imagen del almacenamiento.
        seleccionarFotoTextView.setOnClickListener {
            // Solicitar permisos antes de abrir el selector
            // de imagenes.
            classesStoragePhotos.requestPermissions();

            // Lo que haremos despues de darle click.
            dialog.dismiss();
        }

        dialog.show();
    }

    // Se llama la funcion cuando el usuario responde
    // si acepta los permisos.
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Llamamos la funcion que declaramos en el archivo
        // ClassesCameraPhoto y le pasamos los parametros
        // necesarios.
        classesCameraPhoto.onRequestPermissionsResult(requestCode, grantResults, {
            // Declaramos la variable para traer la funcion
            // dispatch de la clase camera foto.
            val takePictureIntent = classesCameraPhoto.dispatchTakePictureIntent();

            // Aqui ejecutando el tomarFoto podremos
            // iniciar y tomar la foto.
            if (takePictureIntent != null) {
                tomarFoto.launch(takePictureIntent);
            }
        }, {
            // Si hubo un acceso denegado en los permisos
            // mostramos un toast de error.
            Toast.makeText(this, "Acceso Denegado!", Toast.LENGTH_LONG).show();
        });
    }

    // Validar que los campos esten llenos.
    private fun validateFields(
        nombre: String, apellido: String, telefono: String, correo: String, password: String
    ): Boolean {
        // Si alguno de los campos esta vacio.
        if (nombre.isEmpty() || apellido.isEmpty() || telefono.isEmpty() || correo.isEmpty() || password.isEmpty()) {
            // Entonces mostrar el siguiente toast.
            Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()

            // Retornar faslse.
            return false
        }

        // Si los campos estan bien retornar true.
        return true
    }

    // Funcion para mandar un correo de verificacion al usuario.
    private fun sendEmailVerification() {
        // Declaramos una variable para obtener el usuario actual.
        val usuario = auth.currentUser

        // Verificamos que el usuario no sea nulo.
        if (usuario !== null) {
            // Continuamos y mandamos el correo de verificacion.
            usuario.sendEmailVerification().addOnCompleteListener(this) { task ->
                // Si la tarea fue satisfactoria continuar.
                if (task.isSuccessful) {
                    // Mandamos un mensaje de exito al usuario.
                    Toast.makeText(
                        this,
                        "Se ha enviado un correo de verificación a ${usuario.email}. Por favor, revisa tu bandeja de entrada.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Si paso algo malo al crear la cuenta mandar un mensaje de error.
                    Toast.makeText(
                        this, "Hubo un error al crear la cuenta.", Toast.LENGTH_SHORT
                    ).show()
                }
            }.addOnFailureListener(this) { exception ->
                // Si al enviar el correo paso algo malo mostrarle al usuario.
                Toast.makeText(
                    this, "Hubo un error al mandar el correo.", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Funcion para registrar los usuarios.
    private fun registerUser(
        nombre: String,
        apellido: String,
        telefono: String,
        correo: String,
        password: String,
        imageUrl: String
    ) {
        // Guardar el correo y contraseña en
        // firbase auth.
        auth.createUserWithEmailAndPassword(correo, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Mandamos un correo de verificacion.
                sendEmailVerification();

                // Si salio bien entonces declaramos
                // las siguientes variables.
                val user = auth.currentUser;
                val userId = user?.uid;

                // Usamos un hashMap para recoger los
                // valores de cada campo.
                val userMap = hashMapOf(
                    "nombre" to nombre,
                    "apellido" to apellido,
                    "telefono" to telefono,
                    "correo" to correo,
                    "imageUrl" to imageUrl,
                );

                // Si el usuario existe entonces continuamos.
                if (userId != null) {
                    // Guardamos todos los datos del usuario
                    // en una tabla/coleccion llamada users.
                    fireStore.collection("users").document(userId).set(userMap)
                        .addOnSuccessListener {
                            // Mostramos un mensaje si fue correcto el
                            // ingreso de datos.
                            Toast.makeText(this, "Registro satisfactorio.", Toast.LENGTH_SHORT)
                                .show();

                            // Redirigimos a ActivitySignIn.
                            val intent = Intent(this, ActivitySignIn::class.java);
                            startActivity(intent);
                        }.addOnFailureListener { error ->
                            // Si hubo algun error mostrar el siguiente
                            // toast.
                            Toast.makeText(
                                this, "Error al registrarse. ${error.message}", Toast.LENGTH_SHORT
                            ).show();
                        }
                }
            }
        }
    }
}