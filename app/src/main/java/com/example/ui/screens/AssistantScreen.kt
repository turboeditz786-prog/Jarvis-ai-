package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.system.ContactResult
import com.example.ui.JarvisUiState
import com.example.ui.JarvisViewModel
import com.example.ui.components.ArcReactorVisualizer
import com.example.ui.components.JarvisConsoleTerminal
import com.example.ui.theme.ArcBlue
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.ArcGreen
import com.example.ui.theme.ColorCardGlass
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudSurfaceVariant
import com.example.ui.theme.StarkGold
import com.example.ui.theme.TextCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.voice.AssistantListeningState

@Composable
fun AssistantScreen(
  viewModel: JarvisViewModel,
  uiState: JarvisUiState,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var textInput by remember { mutableStateOf("") }
  val scrollState = rememberScrollState()

  // Quick action dialog states
  var showCallDialog by remember { mutableStateOf(false) }
  var callTargetInput by remember { mutableStateOf("") }

  var showMessageDialog by remember { mutableStateOf(false) }
  var msgTargetInput by remember { mutableStateOf("") }
  var msgBodyInput by remember { mutableStateOf("") }
  var isWhatsAppTarget by remember { mutableStateOf(false) }

  // Permission launcher for audio and calls
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { perms ->
    if (perms[Manifest.permission.RECORD_AUDIO] == true) {
      viewModel.toggleVoiceListening()
    }
  }

  fun requestMicAndListen() {
    val hasAudio = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    if (hasAudio) {
      viewModel.toggleVoiceListening()
    } else {
      permissionLauncher.launch(
        arrayOf(
          Manifest.permission.RECORD_AUDIO,
          Manifest.permission.CALL_PHONE,
          Manifest.permission.READ_CONTACTS
        )
      )
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // 1. Arc Reactor Visualizer (Centerpiece)
    Spacer(modifier = Modifier.height(10.dp))
    ArcReactorVisualizer(
      listeningState = uiState.listeningState,
      audioRms = uiState.audioRms,
      onReactorClick = { requestMicAndListen() }
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Interactive State Cue
    val statusText = when (uiState.listeningState) {
      AssistantListeningState.LISTENING -> "● LISTENING… SAY \"CALL JOHN\" OR \"OPEN YOUTUBE\""
      AssistantListeningState.PROCESSING -> "● PROCESSING PROTOCOL WITH NEURAL CORE…"
      AssistantListeningState.SPEAKING -> "● TRANSMITTING AUDIO SYNTHESIS…"
      else -> "TAP REACTOR OR MIC TO ACTIVATE VOICE COMMAND"
    }

    Text(
      text = statusText,
      color = when (uiState.listeningState) {
        AssistantListeningState.LISTENING -> ArcCyan
        AssistantListeningState.PROCESSING -> StarkGold
        else -> TextSecondary
      },
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Terminal Console
    JarvisConsoleTerminal(
      speech = uiState.jarvisSpeech,
      userTranscript = uiState.userTranscript,
      lastAction = uiState.lastExecutedActionText,
      listeningState = uiState.listeningState,
      logs = uiState.executionLogs,
      onStopSpeech = { viewModel.stopSpeaking() }
    )

    // 3. Contact Disambiguation Card (When multiple contacts match)
    AnimatedVisibility(
      visible = uiState.disambiguationContacts.isNotEmpty(),
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Column(modifier = Modifier.padding(top = 14.dp)) {
        ContactDisambiguationCard(
          query = uiState.disambiguationQuery,
          candidates = uiState.disambiguationContacts,
          onSelectContact = { contact ->
            viewModel.callSelectedContact(contact)
          },
          onDismiss = {
            viewModel.dismissDisambiguation()
          }
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Quick Prompt Chips (Horizontal Scroll)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      QuickPromptChip("Call John") { viewModel.executeCommand("Call John") }
      QuickPromptChip("Call +1-555-0101") { viewModel.executeCommand("Call +1-555-0101") }
      QuickPromptChip("Open YouTube") { viewModel.executeCommand("Open YouTube") }
      QuickPromptChip("Message John I'll be late") { viewModel.executeCommand("Send a message to John saying I'll be late") }
      QuickPromptChip("Turn on Wi-Fi") { viewModel.executeCommand("Turn on Wi-Fi") }
      QuickPromptChip("Increase brightness") { viewModel.executeCommand("Increase brightness") }
      QuickPromptChip("Increase volume") { viewModel.executeCommand("Increase volume") }
      QuickPromptChip("Turn on flashlight") { viewModel.executeCommand("Turn on flashlight") }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 5. Action Command Bar (Input Field + Mic Button + Send Button)
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = textInput,
        onValueChange = { textInput = it },
        placeholder = {
          Text(
            "Command Jarvis (e.g. Call John, Open YouTube...)",
            color = TextSecondary,
            fontSize = 12.sp
          )
        },
        modifier = Modifier
          .weight(1f)
          .testTag("command_input"),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = ArcCyan,
          unfocusedBorderColor = HudBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary,
          focusedContainerColor = HudSurfaceVariant,
          unfocusedContainerColor = HudSurfaceVariant
        ),
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(
          onSend = {
            if (textInput.isNotBlank()) {
              viewModel.executeCommand(textInput)
              textInput = ""
            }
          }
        )
      )

      Spacer(modifier = Modifier.width(8.dp))

      // Mic Button
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(if (uiState.listeningState == AssistantListeningState.LISTENING) ArcCyan else HudSurfaceVariant)
          .border(1.dp, ArcCyan, CircleShape)
          .clickable { requestMicAndListen() }
          .testTag("mic_toggle_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (uiState.listeningState == AssistantListeningState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
          contentDescription = "Voice Input",
          tint = if (uiState.listeningState == AssistantListeningState.LISTENING) Color(0xFF070B14) else ArcCyan,
          modifier = Modifier.size(22.dp)
        )
      }

      Spacer(modifier = Modifier.width(6.dp))

      // Send Button
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(ArcCyan)
          .clickable {
            if (textInput.isNotBlank()) {
              viewModel.executeCommand(textInput)
              textInput = ""
            }
          }
          .testTag("send_command_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Send Command",
          tint = Color(0xFF070B14),
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 6. Quick Control Grid
    Text(
      text = "DIRECT PROTOCOLS",
      color = TextSecondary,
      fontSize = 11.sp,
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp,
      modifier = Modifier.align(Alignment.Start)
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      QuickActionCard(
        title = "Voice Call",
        subtitle = "Dial or Contact",
        icon = Icons.Default.Call,
        modifier = Modifier.weight(1f)
      ) {
        showCallDialog = true
      }

      QuickActionCard(
        title = "Dispatch Message",
        subtitle = "SMS / WhatsApp",
        icon = Icons.AutoMirrored.Filled.Chat,
        modifier = Modifier.weight(1f)
      ) {
        showMessageDialog = true
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      QuickActionCard(
        title = "Toggle Flashlight",
        subtitle = if (uiState.isTorchActive) "State: ACTIVE" else "State: OFF",
        icon = Icons.Default.FlashlightOn,
        accentColor = if (uiState.isTorchActive) StarkGold else ArcCyan,
        modifier = Modifier.weight(1f)
      ) {
        viewModel.quickToggleTorch()
      }

      QuickActionCard(
        title = "Device Controls",
        subtitle = "Wi-Fi, Audio & Sys",
        icon = Icons.Default.Settings,
        modifier = Modifier.weight(1f)
      ) {
        viewModel.quickOpenSettings("general")
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
  }

  // Dialog: Direct Call
  if (showCallDialog) {
    AlertDialog(
      onDismissRequest = { showCallDialog = false },
      containerColor = ColorCardGlass,
      title = {
        Text("DIRECT TELEPHONIC LINK", color = ArcCyan, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
      },
      text = {
        Column {
          Text("Specify contact name or phone number:", color = TextSecondary, fontSize = 13.sp)
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = callTargetInput,
            onValueChange = { callTargetInput = it },
            placeholder = { Text("e.g. John, Mom, +1-555-0101", color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ArcCyan,
              unfocusedBorderColor = HudBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (callTargetInput.isNotBlank()) {
              viewModel.quickCall(callTargetInput)
              showCallDialog = false
              callTargetInput = ""
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ArcCyan)
        ) {
          Text("Call Now", color = Color(0xFF070B14), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showCallDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Dialog: Dispatch Message
  if (showMessageDialog) {
    AlertDialog(
      onDismissRequest = { showMessageDialog = false },
      containerColor = ColorCardGlass,
      title = {
        Text("DISPATCH PROTOCOL", color = ArcCyan, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
      },
      text = {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { isWhatsAppTarget = false },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (!isWhatsAppTarget) ArcCyan else HudSurfaceVariant
              ),
              modifier = Modifier.weight(1f)
            ) {
              Text("SMS", color = if (!isWhatsAppTarget) Color(0xFF070B14) else TextPrimary)
            }
            Button(
              onClick = { isWhatsAppTarget = true },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isWhatsAppTarget) ArcCyan else HudSurfaceVariant
              ),
              modifier = Modifier.weight(1f)
            ) {
              Text("WhatsApp", color = if (isWhatsAppTarget) Color(0xFF070B14) else TextPrimary)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = msgTargetInput,
            onValueChange = { msgTargetInput = it },
            placeholder = { Text("Contact Name or Phone", color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ArcCyan,
              unfocusedBorderColor = HudBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(8.dp))
          OutlinedTextField(
            value = msgBodyInput,
            onValueChange = { msgBodyInput = it },
            placeholder = { Text("Message Body (e.g. I'll be late)", color = TextSecondary) },
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = ArcCyan,
              unfocusedBorderColor = HudBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (msgTargetInput.isNotBlank()) {
              val platform = if (isWhatsAppTarget) "WhatsApp" else "SMS"
              viewModel.executeCommand("Send a message to $msgTargetInput saying ${msgBodyInput.ifBlank { "Hello" }}")
              showMessageDialog = false
              msgTargetInput = ""
              msgBodyInput = ""
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ArcCyan)
        ) {
          Text("Transmit", color = Color(0xFF070B14), fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showMessageDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun ContactDisambiguationCard(
  query: String,
  candidates: List<ContactResult>,
  onSelectContact: (ContactResult) -> Unit,
  onDismiss: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(ColorCardGlass)
      .border(1.5.dp, StarkGold, RoundedCornerShape(16.dp))
      .padding(14.dp)
      .testTag("contact_disambiguation_card")
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.ContactPhone,
            contentDescription = null,
            tint = StarkGold,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "MULTIPLE CONTACTS LOCATED",
            color = StarkGold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(24.dp).testTag("dismiss_disambiguation_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Dismiss",
            tint = TextSecondary,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Multiple entries share the name \"$query\". Please choose the desired recipient, Boss:",
        color = TextPrimary,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        candidates.forEachIndexed { index, contact ->
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(HudSurfaceVariant)
              .border(1.dp, HudBorder, RoundedCornerShape(10.dp))
              .clickable { onSelectContact(contact) }
              .padding(horizontal = 12.dp, vertical = 10.dp)
              .testTag("call_candidate_$index")
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ArcCyan.copy(alpha = 0.2f)),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = ArcCyan,
                    modifier = Modifier.size(18.dp)
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = contact.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = contact.phoneNumber,
                    color = TextCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(8.dp))
                  .background(ArcGreen)
                  .padding(horizontal = 10.dp, vertical = 6.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color(0xFF070B14),
                    modifier = Modifier.size(12.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "CALL",
                    color = Color(0xFF070B14),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun QuickPromptChip(text: String, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .background(HudSurfaceVariant)
      .border(1.dp, HudBorder, RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Text(
      text = text,
      color = TextCyan,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
private fun QuickActionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  accentColor: Color = ArcCyan,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(ColorCardGlass)
      .border(1.dp, HudBorder, RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(14.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accentColor,
          modifier = Modifier.size(24.dp)
        )
        Icon(
          imageVector = Icons.AutoMirrored.Filled.OpenInNew,
          contentDescription = null,
          tint = TextSecondary,
          modifier = Modifier.size(14.dp)
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Text(
        text = title,
        color = TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        color = TextSecondary,
        fontSize = 11.sp
      )
    }
  }
}
