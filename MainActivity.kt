package jp.example.yaberu

import android.app.*
import android.content.Intent
import android.net.Uri
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Cream=Color(0xFFF7F9FC); private val Green=Color(0xFF8DB9E8); private val Ink=Color(0xFF263747)

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 31) {
            val am=getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) runCatching { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))) }
        }
        AlarmScheduler.rescheduleAll(this)
        setContent { YaberuApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YaberuApp() {
    val context= LocalContext.current
    var tasks by remember { mutableStateOf(TaskStore.load(context).toList()) }
    var showAdd by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TodoTask?>(null) }
    var selected by remember { mutableStateOf("すべて") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showCalendar by remember { mutableStateOf(false) }
    var showPauseDialog by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var pauseRefresh by remember { mutableIntStateOf(0) }
    val alarmsPaused = remember(pauseRefresh) { AlarmPauseStore.isPaused(context) }
    val today=LocalDate.now()
    val todays=tasks.filter { it.enabled && it.isDueOn(today) }
    val groups=listOf("毎日","毎週","毎月","単発")
    MaterialTheme(colorScheme=lightColorScheme(primary=Green,background=Cream,surface=Cream,onSurface=Ink)) {
        Scaffold(containerColor=Cream, floatingActionButton={ FloatingActionButton(onClick={showAdd=true},containerColor=Green,contentColor=Color.White){ Icon(Icons.Default.Add,"追加") } }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=20.dp)) {
                Spacer(Modifier.height(22.dp))
                Text("やるベル",fontSize=14.sp,color=Green,fontWeight=FontWeight.Bold)
                Text(if(selected=="すべて") "タスク一覧" else if(selected=="日付") selectedDate.format(DateTimeFormatter.ofPattern("M月d日")) else "今日やること",fontSize=30.sp,fontWeight=FontWeight.Bold,color=Ink)
                Text(today.format(DateTimeFormatter.ofPattern("yyyy年M月d日  E曜日")),color=Color.Gray,fontSize=14.sp)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                    Text(if (alarmsPaused) AlarmPauseStore.label(context) else "アラームはオン", color=if (alarmsPaused) Color(0xFFB36B36) else Color.Gray, fontSize=12.sp, modifier=Modifier.weight(1f))
                    TextButton(onClick={showSoundDialog=true}) { Text("アラーム音") }
                    TextButton(onClick={
                        if (alarmsPaused) {
                            AlarmPauseStore.resume(context)
                            pauseRefresh++
                        } else showPauseDialog=true
                    }) { Text(if (alarmsPaused) "再開" else "一時停止") }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("すべて","今日").forEach { label -> FilterChip(selected==label,{selected=label},label={Text(label)}) }
                    TextButton(onClick={ showCalendar = !showCalendar }) { Text(if (showCalendar) "カレンダーを閉じる" else "カレンダー") }
                }
                if (showCalendar) CalendarPanel(tasks=tasks, selectedDate=selectedDate, onSelect={ day -> selectedDate=day; selected="日付"; showCalendar=false })
                Spacer(Modifier.height(4.dp))
                Text("タスク名をタップすると編集できます",fontSize=11.sp,color=Color.Gray)
                Spacer(Modifier.height(8.dp))
                val visible=when(selected) { "今日" -> todays; "日付" -> tasks.filter { it.enabled && it.isDueOn(selectedDate) }; else -> tasks.filter { it.enabled } }
                if(visible.isEmpty()) {
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("☀",fontSize=42.sp); Text("タスクはまだありません",fontWeight=FontWeight.SemiBold); Text("＋から最初のTodoを追加しよう",color=Color.Gray,fontSize=13.sp) } }
                } else LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(bottom=90.dp)) {
                    items(visible,key={it.id}) { task ->
                        val done=(if(selected=="日付") selectedDate else today).toString() in task.completedDates
                        Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White),elevation=CardDefaults.cardElevation(1.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
                                Checkbox(done,{ checked ->
                                    val updated=tasks.map { if(it.id==task.id) { if(checked) it.copy(completedDates=it.completedDates+(if(selected=="日付") selectedDate else today).toString()) else it.copy(completedDates=it.completedDates-(if(selected=="日付") selectedDate else today).toString()) } else it }
                                    tasks=updated; TaskStore.save(context,tasks); AlarmScheduler.rescheduleAll(context)
                                },colors=CheckboxDefaults.colors(checkedColor=Green))
                                Column(Modifier.weight(1f).padding(start=6.dp).clickable { editingTask = task }) {
                                    Text(task.title,fontWeight=FontWeight.SemiBold,fontSize=16.sp,color=if(done) Color.Gray else Ink)
                                    val endLabel = if (task.endHour != null && task.endMinute != null) "%02d:%02d".format(task.endHour, task.endMinute) else "終了未設定"
                                    Text("${"%02d:%02d".format(task.hour,task.minute)} → $endLabel  ·  ${task.repeatLabel()}",fontSize=12.sp,color=Green)
                                }
                                IconButton(onClick={
                                    AlarmScheduler.cancel(context,task); tasks=tasks.filterNot{it.id==task.id}; TaskStore.save(context,tasks)
                                }) { Icon(Icons.Default.Delete,"削除",tint=Color.LightGray) }
                            }
                        }
                    }
                }
            }
        }
        if (showSoundDialog) AlarmSoundDialog(onDismiss={showSoundDialog=false})
        if (showPauseDialog) {
            AlertDialog(
                onDismissRequest={ showPauseDialog=false },
                containerColor=Color.White,
                title={ Text("アラームを一時停止") },
                text={ Text("映画を見るときなど、開始・終了アラームを一時的に止めます。タスクと時刻はそのまま残ります。") },
                confirmButton={
                    Column(horizontalAlignment=Alignment.End) {
                        TextButton(onClick={ AlarmPauseStore.pause(context, 60L*60L*1000L); pauseRefresh++; showPauseDialog=false }) { Text("1時間停止") }
                        TextButton(onClick={ AlarmPauseStore.pause(context, 2L*60L*60L*1000L); pauseRefresh++; showPauseDialog=false }) { Text("2時間停止") }
                        TextButton(onClick={ AlarmPauseStore.pause(context, null); pauseRefresh++; showPauseDialog=false }) { Text("手動で再開するまで停止") }
                    }
                },
                dismissButton={ TextButton(onClick={showPauseDialog=false}) { Text("キャンセル") } }
            )
        }
        if(showAdd) AddTaskDialog(onDismiss={showAdd=false},onSave={task->
            tasks=tasks+task; TaskStore.save(context,tasks); AlarmScheduler.rescheduleAll(context); showAdd=false
        })
        editingTask?.let { current ->
            AddTaskDialog(initialTask=current, onDismiss={editingTask=null}, onSave={updated ->
                tasks=tasks.map { if (it.id == updated.id) updated else it }
                TaskStore.save(context,tasks)
                AlarmScheduler.rescheduleAll(context)
                editingTask=null
            })
        }
    }
}

private fun TodoTask.repeatLabel(): String = when(repeat) { "毎週" -> "毎週 ${weekdays.sorted().joinToString("・") { listOf("","月","火","水","木","金","土","日")[it] }}"; "単発" -> "[${runCatching { LocalDate.parse(date).format(DateTimeFormatter.ofPattern("M/d")) }.getOrDefault(date)}]"; "毎日" -> "[毎日]"; else -> repeat }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(initialTask: TodoTask? = null, onDismiss:()->Unit, onSave:(TodoTask)->Unit) {
    val context=LocalContext.current
    var title by remember(initialTask?.id) { mutableStateOf(initialTask?.title ?: "") }
    var hour by remember(initialTask?.id) { mutableIntStateOf(initialTask?.hour ?: 9) }; var minute by remember(initialTask?.id) { mutableIntStateOf(initialTask?.minute ?: 0) }
    var endHour by remember(initialTask?.id) { mutableIntStateOf(initialTask?.endHour ?: 10) }; var endMinute by remember(initialTask?.id) { mutableIntStateOf(initialTask?.endMinute ?: 0) }
    var repeat by remember(initialTask?.id) { mutableStateOf(initialTask?.repeat ?: "単発") }; var date by remember(initialTask?.id) { mutableStateOf(initialTask?.date ?: LocalDate.now().toString()) }
    var days by remember(initialTask?.id) { mutableStateOf(initialTask?.weekdays ?: setOf(1,2,3,4,5,6,7)) }
    val endTimeValid = LocalTime.of(endHour, endMinute).isAfter(LocalTime.of(hour, minute))
    AlertDialog(onDismissRequest=onDismiss,containerColor=Color.White,title={Text(if (initialTask == null) "Todoを追加" else "Todoを編集",fontWeight=FontWeight.Bold)},text={
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value=title,onValueChange={title=it},label={Text("やること")},singleLine=true)
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("開始",Modifier.weight(1f),color=Ink)
                TextButton(onClick={ TimePickerDialog(context,{_,h,m->hour=h;minute=m},hour,minute,true).show() }) { Icon(Icons.Default.Alarm,null); Spacer(Modifier.width(6.dp)); Text("%02d:%02d".format(hour,minute),fontSize=20.sp) }
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("終了",Modifier.weight(1f),color=Ink)
                TextButton(onClick={ TimePickerDialog(context,{_,h,m->endHour=h;endMinute=m},endHour,endMinute,true).show() }) { Icon(Icons.Default.Alarm,null); Spacer(Modifier.width(6.dp)); Text("%02d:%02d".format(endHour,endMinute),fontSize=20.sp) }
            }
            Text("繰り返し",fontWeight=FontWeight.SemiBold)
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) { listOf("単発","毎日","毎週","毎月").forEach { FilterChip(repeat==it,{repeat=it},label={Text(it,fontSize=12.sp)}) } }
            if(repeat=="単発" || repeat=="毎月") {
                OutlinedTextField(value=date,onValueChange={date=it},label={Text("日付（YYYY-MM-DD）")},singleLine=true)
            }
            if(repeat=="毎週") {
                Row(horizontalArrangement=Arrangement.spacedBy(2.dp)) { listOf("月","火","水","木","金","土","日").forEachIndexed { i,d -> FilterChip(days.contains(i+1),{days=if(days.contains(i+1)) days-(i+1) else days+(i+1)},label={Text(d,fontSize=11.sp)},modifier=Modifier.weight(1f)) } }
            }
            if (!endTimeValid) Text("終了時刻は開始時刻より後に設定してください。",fontSize=12.sp,color=Color(0xFFB36B36))
            Text("開始時刻に「始める」、終了時刻に「今日はここまで」のアラームが鳴ります。",fontSize=12.sp,color=Color.Gray)
        }
    },confirmButton={TextButton(onClick={
        if(title.isNotBlank() && endTimeValid && runCatching{LocalDate.parse(date)}.isSuccess && (repeat!="毎週" || days.isNotEmpty())) {
            onSave(TodoTask(id=initialTask?.id ?: java.util.UUID.randomUUID().toString(), title=title.trim(),hour=hour,minute=minute,repeat=repeat,date=date,weekdays=days,completedDates=initialTask?.completedDates ?: emptySet(),enabled=initialTask?.enabled ?: true,endHour=endHour,endMinute=endMinute))
        }
    }) { Text(if (initialTask == null) "追加" else "保存") }},dismissButton={TextButton(onClick=onDismiss){Text("キャンセル")}})
}


@Composable
fun AlarmSoundDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val options = remember { AlarmSoundStore.available(context) }
    var selected by remember { mutableStateOf(AlarmSoundStore.selected(context).ifEmpty { options.firstOrNull()?.let { setOf(it.uri) } ?: emptySet() }) }
    var preview by remember { mutableStateOf<Ringtone?>(null) }
    AlertDialog(
        onDismissRequest = { preview?.stop(); onDismiss() },
        containerColor = Color.White,
        title = { Text("アラーム音を選ぶ", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("チェックした音から、開始・終了のたびにランダムで再生します。試聴ボタンで音を確認できます。", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                if (options.isEmpty()) Text("この端末で選べるアラーム音が見つかりませんでした。", fontSize = 13.sp)
                else LazyColumn(Modifier.heightIn(max = 340.dp)) {
                    items(options, key = { it.uri }) { option ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = option.uri in selected, onCheckedChange = { checked ->
                                selected = if (checked) selected + option.uri else selected - option.uri
                            })
                            Text(option.title, Modifier.weight(1f), fontSize = 14.sp)
                            TextButton(onClick = {
                                preview?.stop()
                                preview = RingtoneManager.getRingtone(context, Uri.parse(option.uri))
                                preview?.play()
                            }) { Text("試聴") }
                        }
                    }
                }
                if (selected.isEmpty()) Text("少なくとも1つ選択してください。", color = Color(0xFFB36B36), fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = selected.isNotEmpty(), onClick = {
                preview?.stop()
                AlarmSoundStore.saveSelected(context, selected)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = { preview?.stop(); onDismiss() }) { Text("キャンセル") } }
    )
}


@Composable
fun CalendarPanel(tasks: List<TodoTask>, selectedDate: LocalDate, onSelect: (LocalDate) -> Unit) {
    var month by remember(selectedDate.year, selectedDate.monthValue) { mutableStateOf(selectedDate.withDayOfMonth(1)) }
    val first = month.withDayOfMonth(1)
    val offset = first.dayOfWeek.value - 1
    val days = (0 until 42).map { first.minusDays(offset.toLong()).plusDays(it.toLong()) }
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick={ month = month.minusMonths(1) }) { Text("‹") }
                Text(month.format(DateTimeFormatter.ofPattern("yyyy年M月")), Modifier.weight(1f), fontWeight=FontWeight.Bold)
                TextButton(onClick={ month = month.plusMonths(1) }) { Text("›") }
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("月","火","水","木","金","土","日").forEach { day ->
                    Text(day, Modifier.weight(1f), textAlign=androidx.compose.ui.text.style.TextAlign.Center, fontSize=11.sp, color=Color.Gray)
                }
            }
            LazyVerticalGrid(columns=GridCells.Fixed(7), modifier=Modifier.height(240.dp)) {
                items(days) { day ->
                    val inMonth = day.month == month.month && day.year == month.year
                    val count = tasks.count { it.enabled && it.isDueOn(day) }
                    val isSelected = day == selectedDate
                    Column(
                        Modifier.padding(2.dp).fillMaxWidth().height(48.dp)
                            .background(if (isSelected) Color(0xFFDCEBFA) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable(enabled=inMonth) { onSelect(day) }
                            .padding(vertical=4.dp),
                        horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.Center
                    ) {
                        Text(day.dayOfMonth.toString(), fontSize=13.sp, color=if (inMonth) Ink else Color.LightGray)
                        Text(if (count > 0 && inMonth) "${count}件" else " ", fontSize=9.sp, color=Green)
                    }
                }
            }
        }
    }
}
