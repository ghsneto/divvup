package br.app.divvup.notificacoes

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.app.divvup.MainActivity
import br.app.divvup.R
import br.app.divvup.core.domain.notificacao.RepositorioDeNotificacoes
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import br.app.divvup.core.designsystem.R as RDs

private const val CANAL = "avisos"

/** Canal único "Avisos da casa" (o tipo de cada aviso é controlado em E4, R-34). */
fun criarCanalDeAvisos(context: Context) {
    val canal = NotificationChannel(CANAL, context.getString(R.string.canal_avisos), NotificationManager.IMPORTANCE_DEFAULT)
    context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
}

/** FCM: token novo vai para o backend; com o app aberto, o aviso vira notificação do sistema. */
@AndroidEntryPoint
class ServicoDePush : FirebaseMessagingService() {
    @Inject lateinit var notificacoes: RepositorioDeNotificacoes
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Obsoleto no 26.x, mas segue sendo o aviso de token novo do FCM.
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onNewToken(token: String) {
        escopo.launch { notificacoes.registrarAparelho(token) }
    }

    override fun onMessageReceived(mensagem: RemoteMessage) {
        val aviso = mensagem.notification ?: return
        // Antes do Android 13 não há permissão de notificação (R-36).
        val permitido = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!permitido) return
        val abrir = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notificacao = NotificationCompat.Builder(this, CANAL)
            .setSmallIcon(RDs.drawable.ic_sino)
            .setContentTitle(aviso.title)
            .setContentText(aviso.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.body))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(mensagem.messageId.hashCode(), notificacao)
    }

    override fun onDestroy() {
        escopo.cancel()
        super.onDestroy()
    }
}
