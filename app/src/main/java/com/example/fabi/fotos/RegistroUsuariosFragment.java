package com.example.fabi.fotos;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.R.attr.path;


public class RegistroUsuariosFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private  String path;



    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private static  final String CARPETA_PRINCIPAl = "misImagenesApp/";//Directorio Prinicipal
    private static  final String CARPETA_IMAGEN = "imagenes";//Carpeta donde se guardan las fotos
    private static  final String DIRECTORIO_IMAGEN = CARPETA_PRINCIPAl+ CARPETA_IMAGEN;//RUTA CARPETA DE DIRECTORIO

    File fileImagen;//Guarda la foto
    Bitmap bitmap;//Guarda la imagen transformada
    Uri output;
    String fotoCodificada;

    private static final int COD_SELECCIONADA = 10;
    private static final int COD_FOTO = 20;

    EditText campoNombre , campoDocumento, campoProfesion;
    Button botonRegistro, btnFoto;
    ImageView imgFoto;
    ProgressDialog progressDialog;

    RequestQueue request;
    JsonObjectRequest jsonObjectRequest;
    StringRequest stringRequest;

    public RegistroUsuariosFragment() {
        // Required empty public constructor
    }


    public static RegistroUsuariosFragment newInstance(String param1, String param2) {
        RegistroUsuariosFragment fragment = new RegistroUsuariosFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
       View vista = inflater.inflate(R.layout.fragment_registro_usuarios, container, false);

        /* asignacion de controles*/
        campoDocumento = vista.findViewById(R.id.edtDocumento);
        campoNombre = vista.findViewById(R.id.edtNombre);
        campoProfesion = vista.findViewById(R.id.edtProfesion);
        botonRegistro = vista.findViewById(R.id.btnRegistrar);
        btnFoto = vista.findViewById(R.id.btnFoto);
        imgFoto = vista.findViewById(R.id.imgFoto);

        /*Escuchar cuando el boton registro es precionado hace un llamdo al metodo
        * subir imagen*/
        botonRegistro.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SubirImagen();
            }
        });


        /*Llamado de metodo Validar  premisos */

        if (validaPermisos()){
            btnFoto.setEnabled(true);
        }else{
            btnFoto.setEnabled(false);
        }


        /*Inicializacion de variable resquest*/
        request = Volley.newRequestQueue(getContext());

        /*Escuchar si el boon para tomar foto es precionado
        * realiza llamado a metodo mostrarDialogoOpciones*/
        btnFoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mostrarDialogoOpciones();
            }
        });

    return vista;
    }

    /*Metodo para validar los permisos de acceso a la camara y escritura en el almacenamiento del disositivo*/
    private boolean validaPermisos() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return true;
        }
        if ((getContext().checkSelfPermission(CAMERA) == PackageManager.PERMISSION_GRANTED) &&
                (getContext().checkSelfPermission(WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)){
            return true;
        }
        if ((shouldShowRequestPermissionRationale(CAMERA)) || (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE))){
            cargarDialogoRecomendacion();
        }else{
            requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,CAMERA},100);
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode ==100){
            if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED ){
                    btnFoto.setEnabled(true);
            }else{
                solicitarPermisosManual();
            }
        }

    }

    /*Muestra al usuario un mensaje solicitando permiso de acceder a la camara y a guardar datos en la memoria del
    * dispositivo*/
    private void solicitarPermisosManual() {
        final CharSequence[]opciones = {"Si","No"};
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle("¿Desea configurar los permisos de forma manual?");
        builder.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (opciones[i].equals("Si")){
                   Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                   Uri uri = Uri.fromParts("package",getContext().getPackageName(),null);
                    intent.setData(uri);
                    startActivity(intent);
                }else{
                    Toast.makeText(getContext(),"Los permisos no fueron aceptados",Toast.LENGTH_SHORT).show();
                   dialogInterface.dismiss();
                }
            }
        });

        builder.show();
    }

    /*Se muestra al usuario cuando los permisos se encuentran desactivados, y solicita aprovarlos*/

    private void cargarDialogoRecomendacion() {
        AlertDialog.Builder dialogo = new AlertDialog.Builder(getContext());
        dialogo.setTitle("Permisos desactivados");
        dialogo.setMessage("Debe aceptar los permisos para el correcto funcionamiento de la app");

        dialogo.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE,CAMERA},100);

            }
        });
        dialogo.show();

    }
    /*muestra en la pantalla el menu de opciones del cual puede seleccionar tomar una foto o escojer alguna que se encuentre en el
    * dispositivo*/
    private  void mostrarDialogoOpciones(){
        final CharSequence[]opciones = {"Tomar Foto","Elegir de la galeria","Cancelar"};
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

        builder.setTitle("Elige una opcion");
        builder.setItems(opciones, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (opciones[i].equals("Tomar Foto")){
                    abrirCamara();
                }else{
                    if (opciones[i].equals("Elegir de la galeria")){
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        intent.setType("image/");
                        startActivityForResult(intent.createChooser(intent,"Seleccione"),COD_SELECCIONADA);

                    }else{
                        dialogInterface.dismiss();
                    }
                }
            }
        });
        builder.show();
    }



    /*
    *Este metodo se encarga de guardar y dibujar la imagen selecionada por el usuario
    * almasema la imagen y la dibuja en el listview
    * */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case COD_SELECCIONADA:
                Uri miPath = data.getData();
                imgFoto.setImageURI(miPath);
                try {
                    bitmap= MediaStore.Images.Media.getBitmap(getContext().getContentResolver(),miPath);
                    imgFoto.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case COD_FOTO:
                MediaScannerConnection.scanFile(getContext(), new String[]{path},null,
                        new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    Log.i("Path",""+path);
                }
            });
                bitmap = BitmapFactory.decodeFile(path);
                imgFoto.setImageBitmap(bitmap);
                break;
        }
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    /*  METODOS PARA SUBIR LA IMAGEN AL SERVIDOR*/

    private void abrirCamara(){
        File miFile = new File(Environment.getExternalStorageDirectory(),DIRECTORIO_IMAGEN);
        boolean isCreada = miFile.exists();

        if (isCreada == false){
            isCreada = miFile.mkdirs();
        }

        if (isCreada == true){
            Long consecutivo = System.currentTimeMillis()/1000;
            String nombre = consecutivo.toString()+".jpg";
            path = Environment.getExternalStorageDirectory()+File.separator+DIRECTORIO_IMAGEN
                    +File.separator+nombre;
            fileImagen = new File(path);
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(fileImagen));
            startActivityForResult(intent,COD_FOTO);
        }
    }



    public void CargarWebService (){
        progressDialog.hide();
        String url ="http://192.168.1.116/ejemploBDRemota/wsJSONRegistroMovil.php?";

        stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {


            @Override
            //Resive la repuesta del servidorstringRequest
            public void onResponse(String response) {


                /*Comparamos la respueesta del servidor para saber si la imagen
                * el servidor envia una respuesta con la palabra registra para
                * indicar que la imagen se guardo en la BD */

                //trim() es para limpiar los pasios de la cadena y evitar errores de comparacion
                if(response.trim().equalsIgnoreCase("registra")){
                    /*Limpiamos los campos de datos*/
                    campoDocumento.setText("");
                    campoNombre.setText("");
                    campoProfesion.setText("");

                    /*Enviamos el mensaje al usuario que indiaca que la imagen se guardor correctamente*/
                    Toast.makeText(getContext(),"Imagen guardada", Toast.LENGTH_SHORT).show();
                }

            }
            //Se ejecuta el encontrar un error
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                progressDialog.hide();
                Toast.makeText(getContext(),"¡Ups!, algo salio mal",Toast.LENGTH_LONG).show();
                String message = "Error desconocido";

                if (volleyError instanceof NetworkError) {
                    message = "No se puede conectar a Internet\n" + "¡Por favor, compruebe su conexión!";
                } else if (volleyError instanceof ServerError) {
                    message ="El servidor no pudo ser encontrado. \n" +"Por favor, inténtelo mas tarde!";
                } else if (volleyError instanceof AuthFailureError) {
                    message = "No se puede conectar a Internet  \n" +"¡Falla de autenticación!";
                } else if (volleyError instanceof ParseError) {
                    message = "¡Error de sintáxis! \n" +
                               "Error de análisis!";
                } else if (volleyError instanceof NoConnectionError) {
                    message = "No se puede conectar a Internet \n" +
                                    "¡Error de conexión!";
                } else if (volleyError instanceof TimeoutError) {
                    message ="¡El tiempo de conexión expiro! \n" + "\n" +
                                "Error de tiempo de espera";
                }

                Toast.makeText(getContext(),message,Toast.LENGTH_LONG).show();

            }
        }){
           /*Este metodo nos devuelve todos los valores dentro de un map*/
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                String documento = campoDocumento.getText().toString();
                String nombre = campoNombre.getText().toString();
                String profesion = campoProfesion.getText().toString();

                String imagen = ConvertirImagenString(bitmap);



                /*Alimentamos el Map con los datos deseados*/

                Map<String,String> paramemetros = new HashMap<>();
                paramemetros.put("documento",documento);
                paramemetros.put("nombre",nombre);
                paramemetros.put("profesion",profesion);
                paramemetros.put("imagen",imagen);

               // Toast.makeText(getContext(), "Subiendo paramemetros"+paramemetros, Toast.LENGTH_SHORT).show();
                //Regresamos el Map con todos los parametros
                return paramemetros;
            }
        };

        request.add(stringRequest);
    }


    public String ConvertirImagenString(Bitmap bitmap){
        ByteArrayOutputStream array = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,array);
        byte []imagenByte= array.toByteArray();
        String imagenString = Base64.encodeToString(imagenByte,Base64.DEFAULT);
        return imagenString;
    }


    public void SubirImagen() {
        BarraProgreso();
        Toast.makeText(getContext(), "Subiendo Imagen", Toast.LENGTH_SHORT).show();
        CargarWebService();
    }

    public void BarraProgreso(){
        //Inicializa el progres dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle("En Proceso");
        progressDialog.setMessage("Un momento...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
    }
}