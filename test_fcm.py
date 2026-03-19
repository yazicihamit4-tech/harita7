import re
filepath = "izmirharita7/izmirharita7/app/src/main/java/com/yazhamit/izmirharita/MainActivity.kt"
with open(filepath, 'r') as f:
    text = f.read()

# Add a popup for user
block1 = """
                            FirebaseFirestore.getInstance().collection("sinyaller")
                                .document(yeniSinyal.id)
                                .set(yeniSinyal).await()

                            showSuccessDialog = true
"""

# Let's add the success dialog logic and variable
text = text.replace("""
                            FirebaseFirestore.getInstance().collection("sinyaller")
                                .document(yeniSinyal.id)
                                .set(yeniSinyal).await()

                            Toast.makeText(context, "Sinyal Çakıldı! Ekiplerimize iletildi.", Toast.LENGTH_LONG).show()
""", block1)

# Add the showSuccessDialog state variable and the AlertDialog component
text = text.replace("""    var seciliKonum by remember { mutableStateOf<LatLng?>(null) }""", """    var seciliKonum by remember { mutableStateOf<LatLng?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }""")

success_dialog = """
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Text(
                    text = "Başarılı!",
                    color = Color(0xFF388E3C), // Karşıyaka Green
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Sinyal Çakıldı! Ekiplerimize iletildi.",
                    color = Color.Black
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)) // Karşıyaka Red
                ) {
                    Text("Tamam", color = Color.White)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
"""

text = text.replace("""    // ModalBottomSheet ile form""", success_dialog + "\n    // ModalBottomSheet ile form")

with open(filepath, 'w') as f:
    f.write(text)
