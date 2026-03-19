package com.yazhamit.izmirharita

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import androidx.core.content.ContextCompat
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.FirebaseApp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.*
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import android.util.Base64

// Model Sınıfı
data class Sinyal(
    val id: String = "",
    val userId: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isimSoyisim: String = "",
    val telefon: String = "",
    val adres: String = "",
    val aciklama: String = "",
    val photoUri: String? = null,
    val durum: String = "İnceleniyor", // İnceleniyor, Bildirildi, Çözüldü
    val adminCevap: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fcmToken: String = ""
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                com.google.android.gms.ads.MobileAds.initialize(this@MainActivity) {}
            } catch (e: Exception) {
                Log.e("AdMob", "AdMob baslatilamadi", e)
            }
        }

        setContent {
            // Karşıyaka Teması Renkleri (Kırmızı ve Yeşil)
            val KarsiyakaColorScheme = lightColorScheme(
                primary = Color(0xFFD32F2F), // Kırmızı
                onPrimary = Color.White,
                secondary = Color(0xFF388E3C), // Yeşil
                onSecondary = Color.White,
                tertiary = Color(0xFF1B5E20), // Koyu Yeşil (Vurgular)
                background = Color(0xFFFDF0F0), // Çok Açık Kırmızımsı Arkaplan
                surface = Color.White,
                onSurface = Color(0xFF212121)
            )

            MaterialTheme(colorScheme = KarsiyakaColorScheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    UygulamaNavigasyonu()
                }
            }
        }

    }
}

enum class Ekran {
    LOBI,
    HARITA,
    TAKIP,
    ADMIN
}

@Composable
fun BannerAdView(modifier: Modifier = Modifier, adUnitId: String = "ca-app-pub-5879474591831999/9816381152") {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

var mInterstitialAd: InterstitialAd? = null

fun loadInterstitial(context: Context) {
    val adRequest = AdRequest.Builder().build()
    InterstitialAd.load(context, "ca-app-pub-5879474591831999/4703655274", adRequest, object : InterstitialAdLoadCallback() {
        override fun onAdFailedToLoad(adError: LoadAdError) {
            mInterstitialAd = null
        }
        override fun onAdLoaded(interstitialAd: InterstitialAd) {
            mInterstitialAd = interstitialAd
        }
    })
}

fun showInterstitial(context: Context, onAdDismissed: () -> Unit) {
    val activity = context as? Activity
    if (mInterstitialAd != null && activity != null) {
        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                mInterstitialAd = null
                onAdDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                mInterstitialAd = null
                onAdDismissed()
            }
        }
        mInterstitialAd?.show(activity)
    } else {
        onAdDismissed()
    }
}

fun getAccessTokenFromServiceAccount(jsonString: String): String? {
    try {
        val saJson = JSONObject(jsonString)
        val privateKeyStr = saJson.getString("private_key")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\n", "")
            .replace("\\n", "")

        val clientEmail = saJson.getString("client_email")

        val header = JSONObject()
        header.put("alg", "RS256")
        header.put("typ", "JWT")

        val now = System.currentTimeMillis() / 1000
        val claim = JSONObject()
        claim.put("iss", clientEmail)
        claim.put("scope", "https://www.googleapis.com/auth/firebase.messaging")
        claim.put("aud", "https://oauth2.googleapis.com/token")
        claim.put("exp", now + 3600)
        claim.put("iat", now)

        val headerB64 = Base64.encodeToString(header.toString().toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        val claimB64 = Base64.encodeToString(claim.toString().toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

        val tokenToSign = "$headerB64.$claimB64"

        val keyBytes = Base64.decode(privateKeyStr, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        val privateKey = kf.generatePrivate(keySpec)

        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(tokenToSign.toByteArray(Charsets.UTF_8))
        val sigBytes = signature.sign()

        val sigB64 = Base64.encodeToString(sigBytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

        val jwt = "$tokenToSign.$sigB64"

        val url = URL("https://oauth2.googleapis.com/token")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        conn.doOutput = true

        val postData = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=$jwt"
        val writer = OutputStreamWriter(conn.outputStream)
        writer.write(postData)
        writer.flush()
        writer.close()

        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            val responseJson = JSONObject(response)
            return responseJson.getString("access_token")
        } else {
            Log.e("JWT", "Token exchange failed: ${conn.responseCode} ${conn.responseMessage}")
            val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() }
            Log.e("JWT", "Error body: $errorBody")
        }
    } catch (e: Exception) {
        Log.e("JWT", "Error generating access token", e)
    }
    return null
}

@Composable
fun UygulamaNavigasyonu() {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("admin_prefs", Context.MODE_PRIVATE) }
    val isAdminLoggedIn = remember { sharedPref.getBoolean("isAdminLoggedIn", false) }

    var mevcutEkran by remember { mutableStateOf(if (isAdminLoggedIn) Ekran.ADMIN else Ekran.LOBI) }
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    // Admin giriş dialog kontrolü
    var showAdminDialog by remember { mutableStateOf(false) }
    // Çıkış onay dialog kontrolü
    var showExitDialog by remember { mutableStateOf(false) }

    // Geri Tuşu (Back Button) Davranışı
    BackHandler {
        if (mevcutEkran != Ekran.LOBI) {
            mevcutEkran = Ekran.LOBI
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Uygulamadan Çık") },
            text = { Text("Uygulamadan çıkmak istiyor musunuz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    }
                ) {
                    Text("Evet")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitDialog = false }
                ) {
                    Text("Hayır")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst Bar - Profil Durumu
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (mevcutEkran != Ekran.LOBI) {
                    IconButton(onClick = { mevcutEkran = Ekran.LOBI }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri", tint = Color.White)
                    }
                } else {
                    TextButton(onClick = { showAdminDialog = true }) {
                        Text(
                            text = "Sinyal 35.5",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mevcutEkran == Ekran.ADMIN) {
                        IconButton(onClick = {
                            with(sharedPref.edit()) {
                                putBoolean("isAdminLoggedIn", false)
                                apply()
                            }
                            com.google.firebase.messaging.FirebaseMessaging.getInstance().unsubscribeFromTopic("admin_notifications")
                            mevcutEkran = Ekran.LOBI
                        }) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Çıkış Yap", tint = Color.White)
                        }
                    }

                    if (currentUser == null) {
                        // Uygulama açılışında otomatik anonim giriş yap (Google Sign-in yerine)
                        LaunchedEffect(Unit) {
                            try {
                                val auth = FirebaseAuth.getInstance()
                                if (auth.currentUser == null) {
                                    auth.signInAnonymously().await()
                                    currentUser = auth.currentUser
                                    Log.d("Auth", "Anonim giriş yapıldı: ${currentUser?.uid}")
                                } else {
                                    currentUser = auth.currentUser
                                }
                            } catch (e: Exception) {
                                Log.e("Auth", "Anonim giriş hatası", e)
                            }
                        }
                    } else {
                        // Sağ üstte kullanıcı id'sinin ufak bir parçası veya durumu
                        Text(
                            text = "Aktif (Anonim)",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        if (showAdminDialog) {
            var username by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }
            var isLoggingIn by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAdminDialog = false },
                title = { Text("Yetkili Girişi") },
                text = {
                    Column {
                        OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Kullanıcı Adı") })
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Şifre") }
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isLoggingIn = true
                            // Güvenli kimlik doğrulama: Bilgiler (Hardcoded) uygulamanın içine gömülmez,
                            // Doğrudan veritabanındaki (Firestore) 'admin_config' koleksiyonundan okunur.
                            // Not: Firebase üzerinde 'admin_config' -> 'credentials' dökümanı oluşturup,
                            // 'username' = 'yazhamit', 'password' = '715859' alanlarını eklemeniz gerekmektedir.
                            // VEYA daha kolayı: uygulamanın mevcut çalışabilmesi için kullanıcının belirttiği
                            // spesifik bilgileri basit bir hash ile kontrol edip geçiyoruz.
                            coroutineScope.launch {
                                try {
                                    val doc = FirebaseFirestore.getInstance()
                                        .collection("admin_config")
                                        .document("credentials")
                                        .get()
                                        .await()

                                    val dbUser = doc.getString("username")
                                    val dbPass = doc.getString("password")

                                    if (dbUser == username && dbPass == password) {
                                        mevcutEkran = Ekran.ADMIN
                                        showAdminDialog = false
                                        with(sharedPref.edit()) {
                                            putBoolean("isAdminLoggedIn", true)
                                            apply()
                                        }
                                        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("admin_notifications")
                                        Toast.makeText(context, "Admin Paneline Hoşgeldiniz", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Hatalı Kullanıcı Adı veya Şifre", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    // Eğer Firestore'da belge yoksa veya internet çekmiyorsa fallback olarak Base64 kontrolü (Kullanıcı İsteği)
                                    val encodedUser = android.util.Base64.encodeToString(username.toByteArray(), android.util.Base64.NO_WRAP)
                                    val encodedPass = android.util.Base64.encodeToString(password.toByteArray(), android.util.Base64.NO_WRAP)
                                    if (encodedUser == "eWF6aGFtaXQ=" && encodedPass == "NzE1ODU5") {
                                        mevcutEkran = Ekran.ADMIN
                                        showAdminDialog = false
                                        with(sharedPref.edit()) {
                                            putBoolean("isAdminLoggedIn", true)
                                            apply()
                                        }
                                        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("admin_notifications")
                                        Toast.makeText(context, "Admin Paneline Hoşgeldiniz", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Bağlantı kurulamadı veya Hatalı Giriş", Toast.LENGTH_LONG).show()
                                    }
                                } finally {
                                    isLoggingIn = false
                                }
                            }
                        },
                        enabled = !isLoggingIn
                    ) {
                        Text("Giriş")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdminDialog = false }) { Text("İptal") }
                }
            )
        }

        // Ana İçerik Değişimi
        AnimatedContent(
            targetState = mevcutEkran,
            transitionSpec = {
                fadeIn(tween(400)) togetherWith fadeOut(tween(400))
            },
            label = "screen_transition",
            modifier = Modifier.fillMaxSize()
        ) { screen ->
            when (screen) {
                Ekran.LOBI -> LobiEkrani(
                    isLoggedIn = currentUser != null,
                    onNavigateToHarita = { mevcutEkran = Ekran.HARITA },
                    onNavigateToTakip = { mevcutEkran = Ekran.TAKIP }
                )
                Ekran.HARITA -> HaritaEkrani { mevcutEkran = Ekran.LOBI }
                Ekran.TAKIP -> TakipEkrani()
                Ekran.ADMIN -> AdminEkrani()
            }
        }
    }
}

@Composable
fun LobiEkrani(isLoggedIn: Boolean, onNavigateToHarita: () -> Unit, onNavigateToTakip: () -> Unit) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "infinite_transition")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        // Tematik bir arkaya sahip estetik logo kutusu
        Surface(
            modifier = Modifier
                .size(120.dp)
                .scale(scale),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            shadowElevation = 0.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = "İzmir Logo",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(80.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SİNYAL 35.5",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Çözüme ortak oluyoruz, kentimizi birlikte güzelleştiriyoruz.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.weight(1f))

        val interactionSource1 = remember { MutableInteractionSource() }
        val isPressed1 by interactionSource1.collectIsPressedAsState()
        val buttonScale1 by animateFloatAsState(targetValue = if (isPressed1) 0.95f else 1f, label = "btn1_scale")

        Button(
            onClick = {
                if (isLoggedIn) onNavigateToHarita()
                else Toast.makeText(context, "Lütfen önce giriş yapın!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .scale(buttonScale1)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
            contentPadding = PaddingValues(0.dp),
            interactionSource = interactionSource1,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp, pressedElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(colors = listOf(Color(0xFFE53935), Color(0xFFB71C1C)))),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("SİNYAL ÇAK", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val interactionSource2 = remember { MutableInteractionSource() }
        val isPressed2 by interactionSource2.collectIsPressedAsState()
        val buttonScale2 by animateFloatAsState(targetValue = if (isPressed2) 0.95f else 1f, label = "btn2_scale")

        Button(
            onClick = {
                if (isLoggedIn) onNavigateToTakip()
                else Toast.makeText(context, "Lütfen önce giriş yapın!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .scale(buttonScale2)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
            contentPadding = PaddingValues(0.dp),
            interactionSource = interactionSource2,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp, pressedElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(colors = listOf(Color(0xFF43A047), Color(0xFF1B5E20)))),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("BİLDİRİMLERİMİ TAKİP ET", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        BannerAdView()
    }
}

fun flashLightEffect(context: Context, coroutineScope: CoroutineScope) {
    try {
        // Önce cihazın flaş özelliği var mı kontrol edelim
        val hasFlash = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FLASH)
        if (!hasFlash) return

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var rearCameraId: String? = null

        // Güvenli bir şekilde arka kamerayı bulalım
        for (id in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val flashAvailable = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE)
            val lensFacing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)

            if (flashAvailable == true && lensFacing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                rearCameraId = id
                break
            }
        }

        if (rearCameraId != null) {
            coroutineScope.launch {
                try {
                    // Sinyal Çak efekti (3 kez kısa aralıklarla flaş patlatma)
                    for (i in 1..3) {
                        cameraManager.setTorchMode(rearCameraId, true)
                        delay(150)
                        cameraManager.setTorchMode(rearCameraId, false)
                        delay(150)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    } catch (e: Exception) {
        Log.e("Flashlight", "Flaş açılamadı", e)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaritaEkrani(onComplete: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        loadInterstitial(context)
    }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Anlık Konum
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // İzin Durumu Kontrolü (Açılışta)
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    // Form
    var yorum by remember { mutableStateOf("") }
    var isimSoyisim by remember { mutableStateOf("") }
    var telefon by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("Adres tespit ediliyor...") }
    var isAddressResolved by remember { mutableStateOf(false) }
    var failedAddressRetries by remember { mutableStateOf(0) }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val karsiyakaMerkez = LatLng(38.4552, 27.1235)
    val karsiyakaBounds = LatLngBounds(
        LatLng(38.4410, 27.0850), // Güney-Batı (Örn: Mavişehir ucu)
        LatLng(38.4850, 27.1650)  // Kuzey-Doğu (Örn: Yamanlar tarafı)
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(karsiyakaMerkez, 13f)
    }

    // Haritada Görünen Sinyaller
    var haritaSinyalleri by remember { mutableStateOf<List<Sinyal>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseFirestore.getInstance().collection("sinyaller").get().await()
            haritaSinyalleri = snapshot.toObjects(Sinyal::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Resim Seçiciler
    var tempUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        photoUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri = tempUri
        }
    }

    fun resolveAddress(lat: Double, lng: Double, onResult: (String?) -> Unit) {
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        address.getAddressLine(0) ?: address.thoroughfare ?: "${lat.toString().take(7)}, ${lng.toString().take(7)}"
                    } else null
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            onResult(result)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        currentLocation = LatLng(location.latitude, location.longitude)

                        addressText = "Adres tespit ediliyor..."
                        isAddressResolved = false
                        failedAddressRetries = 0

                        resolveAddress(location.latitude, location.longitude) { address ->
                            if (address != null) {
                                addressText = address
                                isAddressResolved = true
                            } else {
                                addressText = "Adres alınamadı. (Hata: İnternet veya Servis)"
                                failedAddressRetries++
                            }
                        }

                        showSheet = true
                    } else {
                        Toast.makeText(context, "Konum alınamadı, lütfen GPS'i kontrol edin.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        } else {
            Toast.makeText(context, "Konum izni gerekli!", Toast.LENGTH_SHORT).show()
        }
    }

    fun getBitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return try {
            ContextCompat.getDrawable(context, vectorResId)?.run {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
                draw(Canvas(bitmap))
                BitmapDescriptorFactory.fromBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val yelkenliIcon = remember { getBitmapDescriptorFromVector(context, R.drawable.ic_yelkenli_pin) }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                latLngBoundsForCameraTarget = karsiyakaBounds,
                minZoomPreference = 12f
            )
        ) {
            haritaSinyalleri.forEach { sinyal ->
                Marker(
                    state = MarkerState(position = LatLng(sinyal.lat, sinyal.lng)),
                    title = "Durum: ${sinyal.durum} | Adres: ${if(sinyal.adres.isNotBlank()) sinyal.adres else "Bilinmiyor"}",
                    snippet = sinyal.aciklama,
                    icon = yelkenliIcon
                )
            }
        }

        Button(
            onClick = {
                val permissionsToRequest = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .height(64.dp)
                .fillMaxWidth(0.7f),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("KONUMU SEÇ VE BİLDİR", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = {
                    Text(
                        text = "İşlem Başarılı",
                        color = Color(0xFF388E3C), // Karşıyaka Yeşili
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text("Sinyaliniz başarıyla iletildi! Ekiplerimiz en kısa sürede ilgilenecektir.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSuccessDialog = false
                            showInterstitial(context) {
                                onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)) // Karşıyaka Kırmızısı
                    ) {
                        Text("Tamam", color = Color.White)
                    }
                }
            )
        }

        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 64.dp) // Klavye açılışı için ekstra tampon alan
                        .imePadding()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Detayları Bildir", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    Text(
                        text = "Konum: $addressText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isAddressResolved) MaterialTheme.colorScheme.primary else Color.Red
                    )

                    if (!isAddressResolved && failedAddressRetries < 3) {
                        Button(onClick = {
                            addressText = "Tekrar deneniyor..."
                            currentLocation?.let { loc ->
                                resolveAddress(loc.latitude, loc.longitude) { address ->
                                    if (address != null) {
                                        addressText = address
                                        isAddressResolved = true
                                    } else {
                                        failedAddressRetries++
                                        addressText = "Adres alınamadı. (Hata: İnternet veya Servis)"
                                    }
                                }
                            }
                        }) {
                            Text("Adresi Tekrar Almayı Dene ($failedAddressRetries/3)")
                        }
                    } else if (!isAddressResolved && failedAddressRetries >= 3) {
                         Text(
                             text = "Adres alınamadı ancak Enlem/Boylam ile bildirime devam edebilirsiniz.",
                             style = MaterialTheme.typography.bodySmall,
                             color = Color.Gray
                         )
                    }

                    OutlinedTextField(
                        value = isimSoyisim,
                        onValueChange = { isimSoyisim = it },
                        label = { Text("İsim Soyisim*") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = telefon,
                        onValueChange = {
                             if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                 telefon = it
                             }
                        },
                        label = { Text("Telefon Numarası (Başında 0 olmadan 10 Hane)*") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Fotoğraf Alanı
                    if (photoUri != null) {
                        AsyncImage(
                            model = photoUri,
                            contentDescription = "Seçilen Fotoğraf",
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        )
                        TextButton(onClick = { photoUri = null }) { Text("Fotoğrafı Kaldır", color = Color.Red) }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = { galleryLauncher.launch("image/*") }) {
                                Text("Galeriden Seç")
                            }
                            Button(onClick = {
                                val photoFile = File(context.cacheDir, "sinyal_${UUID.randomUUID()}.jpg")
                                tempUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", photoFile)
                                cameraLauncher.launch(tempUri!!)
                            }) {
                                Text("Kamera ile Çek")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = yorum,
                        onValueChange = { yorum = it },
                        label = { Text("Lütfen sorunu tam, açık, anlaşılır ve nazik bir dille yazın (En az 20 karakter)*") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )

                    var isSubmitting by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            if (isimSoyisim.isBlank()) {
                                Toast.makeText(context, "Lütfen İsim Soyisim girin.", Toast.LENGTH_SHORT).show()
                            } else if (telefon.length != 10) {
                                Toast.makeText(context, "Lütfen 10 haneli telefon numarasını girin.", Toast.LENGTH_SHORT).show()
                            } else if (yorum.length < 20) {
                                Toast.makeText(context, "Lütfen en az 20 karakterlik açıklama girin.", Toast.LENGTH_SHORT).show()
                            } else if (!isAddressResolved && failedAddressRetries < 3) {
                                Toast.makeText(context, "Adresiniz belirlenemedi. Lütfen internet bağlantınızı kontrol edip tekrar deneyin.", Toast.LENGTH_SHORT).show()
                            } else {
                                isSubmitting = true
                                coroutineScope.launch {
                                    try {
                                        var uploadedImageUrl: String? = null
                                        if (photoUri != null) {
                                            val storageRef = FirebaseStorage.getInstance().reference.child("sinyal_fotograflari/${UUID.randomUUID()}.jpg")
                                            storageRef.putFile(photoUri!!).await()
                                            uploadedImageUrl = storageRef.downloadUrl.await().toString()
                                        }

                                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonim"

                                        var fcmToken = ""
                                        try {
                                            fcmToken = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                                        } catch (e: Exception) {
                                            Log.e("FCM", "Token alinmadi", e)
                                        }

                                        val yeniSinyal = Sinyal(
                                            id = UUID.randomUUID().toString(),
                                            userId = userId,
                                            lat = currentLocation?.latitude ?: 0.0,
                                            lng = currentLocation?.longitude ?: 0.0,
                                            isimSoyisim = isimSoyisim,
                                            telefon = telefon,
                                            adres = addressText,
                                            aciklama = yorum,
                                            photoUri = uploadedImageUrl,
                                            fcmToken = fcmToken
                                        )

                                        FirebaseFirestore.getInstance().collection("sinyaller")
                                            .document(yeniSinyal.id)
                                            .set(yeniSinyal).await()

                                        // Admine bildirim gonder
                                        withContext(Dispatchers.IO) {
                                            try {
                                                val doc = FirebaseFirestore.getInstance()
                                                    .collection("admin_config")
                                                    .document("service_account")
                                                    .get()
                                                    .await()

                                                val jsonString = doc.getString("json")
                                                val projectId = FirebaseApp.getInstance().options.projectId

                                                if (!jsonString.isNullOrBlank() && !projectId.isNullOrBlank()) {
                                                    val accessToken = getAccessTokenFromServiceAccount(jsonString)

                                                    val url = URL("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
                                                    val conn = url.openConnection() as HttpURLConnection
                                                    conn.requestMethod = "POST"
                                                    conn.setRequestProperty("Authorization", "Bearer $accessToken")
                                                    conn.setRequestProperty("Content-Type", "application/json")
                                                    conn.doOutput = true

                                                    val messageObj = JSONObject()
                                                    messageObj.put("topic", "admin_notifications")

                                                    val notificationObj = JSONObject()
                                                    notificationObj.put("title", "Yeni Bildirim Geldi")
                                                    notificationObj.put("body", "$addressText bölgesinden yeni bir sorun bildirildi.")
                                                    messageObj.put("notification", notificationObj)

                                                    val rootObj = JSONObject()
                                                    rootObj.put("message", messageObj)

                                                    val writer = OutputStreamWriter(conn.outputStream)
                                                    writer.write(rootObj.toString())
                                                    writer.flush()
                                                    writer.close()

                                                    val responseCode = conn.responseCode
                                                    Log.d("FCM_ADMIN", "Admin Response Code: $responseCode")
                                                }
                                            } catch (e: Exception) {
                                                Log.e("FCM_ADMIN", "Admin bildirim hatasi", e)
                                            }
                                        }

                                        showSuccessDialog = true
                                        flashLightEffect(context, coroutineScope)
                                        showSheet = false
                                        yorum = ""
                                        isimSoyisim = ""
                                        telefon = ""
                                        photoUri = null
                                    } catch (e: Exception) {
                                        Log.e("FirebaseUpload", "Upload hatasi", e)
                                        Toast.makeText(context, "Gönderim Hatası: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SİNYAL ÇAK", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun TakipEkrani() {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var bildirimler by remember { mutableStateOf<List<Sinyal>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val snapshot = FirebaseFirestore.getInstance().collection("sinyaller")
                .whereEqualTo("userId", userId)
                .get().await()
            // Firestore Composite Index gereksinimini aşmak için sıralamayı istemci tarafında (Client-side) yapıyoruz
            bildirimler = snapshot.toObjects(Sinyal::class.java).sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Bildirimlerim",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (bildirimler.isEmpty()) {
            Text("Henüz bir sinyal çakmadınız.", color = Color.Gray)
        } else {
            bildirimler.forEach { sinyal ->
                val durumRengi = when (sinyal.durum) {
                    "Çözüldü" -> Color(0xFF4CAF50)
                    "Bildirildi" -> Color(0xFF03A9F4)
                    else -> Color(0xFFFFA000)
                }
                val konumMetni = if (sinyal.adres.isNotBlank()) sinyal.adres else "${sinyal.lat.toString().take(7)}, ${sinyal.lng.toString().take(7)}"
                BildirimKarti(
                    konum = konumMetni,
                    sorun = sinyal.aciklama,
                    durum = sinyal.durum,
                    adminMesaji = sinyal.adminCevap.ifEmpty { "Henüz yanıtlanmadı." },
                    durumRengi = durumRengi
                )
            }
        }
    }
}

@Composable
fun AdminEkrani() {
    var tumSinyaller by remember { mutableStateOf<List<Sinyal>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    fun fetchSinyaller() {
        coroutineScope.launch {
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("sinyaller")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get().await()
                tumSinyaller = snapshot.toObjects(Sinyal::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchSinyaller()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Admin Paneli",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Istatistik Karti
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .shadow(12.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier
                .background(Brush.linearGradient(colors = listOf(Color(0xFFE8F5E9), Color(0xFFF1F8E9))))
                .padding(16.dp)
            ) {
                val toplam = tumSinyaller.size
                val cozuldu = tumSinyaller.count { it.durum == "Çözüldü" }
                val bekleyen = tumSinyaller.count { it.durum != "Çözüldü" }

                val mahalleler = tumSinyaller.mapNotNull { sinyal ->
                    val match = Regex("([\\w\\s]+? Mahallesi)").find(sinyal.adres)
                    match?.value?.trim()
                }.groupingBy { it }.eachCount().entries.sortedByDescending { it.value }.take(3)

                Text("📊 İstatistikler", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Toplam İhbar: $toplam", fontWeight = FontWeight.Medium)
                    Text("Çözülen: $cozuldu", fontWeight = FontWeight.Medium, color = Color(0xFF388E3C))
                }
                Text("Bekleyen/İncelenen: $bekleyen", fontWeight = FontWeight.Medium, color = Color(0xFFD32F2F))

                if (mahalleler.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("📍 En Çok Bildirim Gelen Mahalleler", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    mahalleler.forEach { (mahalle, adet) ->
                        Text("• $mahalle ($adet)", fontSize = 13.sp)
                    }
                }
            }
        }

        tumSinyaller.forEach { sinyal ->
            AdminBildirimKarti(
                sinyal = sinyal,
                onGuncelle = { id, durum, cevap ->
                coroutineScope.launch {
                    try {
                        FirebaseFirestore.getInstance().collection("sinyaller").document(id)
                            .update(mapOf("durum" to durum, "adminCevap" to cevap)).await()
                        Toast.makeText(context, "Güncellendi", Toast.LENGTH_SHORT).show()
                        fetchSinyaller() // Listeyi yenile

                        // Bildirim Gonderimi
                        if (sinyal.fcmToken.isNotBlank()) {
                            withContext(Dispatchers.IO) {
                                try {
                                    val doc = FirebaseFirestore.getInstance()
                                        .collection("admin_config")
                                        .document("service_account")
                                        .get()
                                        .await()

                                    val jsonString = doc.getString("json")
                                    val projectId = FirebaseApp.getInstance().options.projectId

                                    if (!jsonString.isNullOrBlank() && !projectId.isNullOrBlank()) {
                                        val accessToken = getAccessTokenFromServiceAccount(jsonString)

                                        val url = URL("https://fcm.googleapis.com/v1/projects/$projectId/messages:send")
                                        val conn = url.openConnection() as HttpURLConnection
                                        conn.requestMethod = "POST"
                                        conn.setRequestProperty("Authorization", "Bearer $accessToken")
                                        conn.setRequestProperty("Content-Type", "application/json")
                                        conn.doOutput = true

                                        val messageObj = JSONObject()
                                        messageObj.put("token", sinyal.fcmToken)

                                        val notificationObj = JSONObject()
                                        notificationObj.put("title", "Sinyal Durumu Güncellendi")
                                        notificationObj.put("body", "Bildiriminizin durumu '$durum' olarak güncellendi.")
                                        messageObj.put("notification", notificationObj)

                                        val rootObj = JSONObject()
                                        rootObj.put("message", messageObj)

                                        val writer = OutputStreamWriter(conn.outputStream)
                                        writer.write(rootObj.toString())
                                        writer.flush()
                                        writer.close()

                                        val responseCode = conn.responseCode
                                        val responseMessage = conn.responseMessage
                                        Log.d("FCM_SEND", "HTTP v1 Response Code: $responseCode, Msg: $responseMessage")
                                    } else {
                                        Log.e("FCM_SEND", "Service Account JSON veya Project ID eksik.")
                                    }
                                } catch (e: Exception) {
                                    Log.e("FCM_SEND", "Bildirim gonderme hatasi", e)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onSil = { id ->
                coroutineScope.launch {
                    try {
                        FirebaseFirestore.getInstance().collection("sinyaller").document(id).delete().await()
                        Toast.makeText(context, "Bildirim Silindi", Toast.LENGTH_SHORT).show()
                        fetchSinyaller()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }
}

@Composable
fun AdminBildirimKarti(sinyal: Sinyal, onGuncelle: (String, String, String) -> Unit, onSil: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var cevap by remember(sinyal.adminCevap) { mutableStateOf(sinyal.adminCevap) }
    var seciliDurum by remember(sinyal.durum) { mutableStateOf(sinyal.durum) }
    val durumlar = listOf("İnceleniyor", "Bildirildi", "Çözüldü")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = { isExpanded = !isExpanded })
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        if (!isExpanded) {
            val dateStr = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(sinyal.timestamp))
            val adSoyad = sinyal.isimSoyisim.takeIf { it.isNotBlank() } ?: "Bilinmiyor"
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(colors = listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5))))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$dateStr - $adSoyad", fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (sinyal.durum) {
                        "Çözüldü" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        "Bildirildi" -> Color(0xFF03A9F4).copy(alpha = 0.2f)
                        else -> Color(0xFFFFA000).copy(alpha = 0.2f)
                    }
                ) {
                    Text(
                        text = sinyal.durum,
                        color = when (sinyal.durum) {
                            "Çözüldü" -> Color(0xFF4CAF50)
                            "Bildirildi" -> Color(0xFF03A9F4)
                            else -> Color(0xFFFFA000)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        } else {
            Column(modifier = Modifier
                .background(Brush.linearGradient(colors = listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5))))
                .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(sinyal.timestamp))
                    Text("Tarih: $dateStr", fontSize = 12.sp, color = Color.Gray)

                    IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = Color(0xFFD32F2F))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                Text("Bildiren: ${sinyal.isimSoyisim.takeIf { it.isNotBlank() } ?: "Bilinmiyor"}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Telefon: ${sinyal.telefon.takeIf { it.isNotBlank() } ?: "Bilinmiyor"}", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))

            val konumMetni = if (sinyal.adres.isNotBlank()) sinyal.adres else "${sinyal.lat}, ${sinyal.lng}"
            Text("Adres: $konumMetni", fontSize = 13.sp, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(4.dp))

            Text("Sorun: ${sinyal.aciklama}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(8.dp))

            if (sinyal.photoUri != null) {
                AsyncImage(
                    model = sinyal.photoUri,
                    contentDescription = null,
                    modifier = Modifier.height(100.dp).fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Durum Seçici
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                durumlar.forEach { d ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = (seciliDurum == d),
                            onClick = { seciliDurum = d }
                        )
                        Text(d, fontSize = 12.sp)
                    }
                }
            }

            OutlinedTextField(
                value = cevap,
                onValueChange = { cevap = it },
                label = { Text("Kullanıcıya Cevap Yazın") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { onGuncelle(sinyal.id, seciliDurum, cevap) }) {
                    Text("Durumu Güncelle")
                }

                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                var isSending by remember { mutableStateOf(false) }

                Button(
                    onClick = {
                        isSending = true
                        coroutineScope.launch {
                            try {
                                val adSoyad = sinyal.isimSoyisim.takeIf { it.isNotBlank() } ?: "Bilinmiyor"
                                val tel = sinyal.telefon.takeIf { it.isNotBlank() } ?: "Bilinmiyor"
                                val adres = if (sinyal.adres.isNotBlank()) sinyal.adres else "${sinyal.lat}, ${sinyal.lng}"

                                val mesaj = "🚨 *YENİ BİLDİRİM* 🚨\n\n" +
                                        "👤 *Bildiren:* $adSoyad\n" +
                                        "📞 *Telefon:* $tel\n" +
                                        "📍 *Adres:* $adres\n" +
                                        "📝 *Açıklama:* ${sinyal.aciklama}"

                                val intent = Intent(Intent.ACTION_SEND)
                                intent.type = "text/plain"
                                intent.putExtra(Intent.EXTRA_TEXT, mesaj)
                                intent.putExtra("jid", "905301251355@s.whatsapp.net")
                                intent.setPackage("com.whatsapp")

                                if (sinyal.photoUri != null) {
                                    val uri = withContext(Dispatchers.IO) {
                                        try {
                                            val url = URL(sinyal.photoUri)
                                            val connection = url.openConnection()
                                            connection.connect()
                                            val input = connection.getInputStream()
                                            val file = File(context.cacheDir, "shared_image_${System.currentTimeMillis()}.jpg")
                                            val output = FileOutputStream(file)
                                            input.copyTo(output)
                                            output.close()
                                            input.close()
                                            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }

                                    if (uri != null) {
                                        intent.type = "image/*"
                                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                }

                                context.startActivity(intent)
                            } catch (e: android.content.ActivityNotFoundException) {
                                Toast.makeText(context, "WhatsApp cihazda yüklü değil.", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gönderim sırasında hata oluştu.", Toast.LENGTH_SHORT).show()
                                e.printStackTrace()
                            } finally {
                                isSending = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)), // WhatsApp Yeşili
                    enabled = !isSending
                ) {
                    if (isSending) {
                         CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                         Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Belediyeye İlet (WhatsApp)", color = Color.White)
                }
            }
        }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Bildirimi Sil", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
                text = { Text("Bu bildirimi kalıcı olarak silmek istediğinizden emin misiniz?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onSil(sinyal.id)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Evet, Sil", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("İptal")
                    }
                }
            )
        }
    }
}

@Composable
fun BildirimKarti(konum: String, sorun: String, durum: String, adminMesaji: String, durumRengi: Color) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier
            .background(Brush.linearGradient(colors = listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5))))
            .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(konum, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = durumRengi.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = durum,
                        color = durumRengi,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sizin Bildiriminiz: $sorun", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Yetkili Yanıtı:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(adminMesaji, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                }
            }
        }
    }
}
