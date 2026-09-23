package com.tution.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tution.app.data.AttendanceEntity
import com.tution.app.data.FeeAdjustmentEntity
import com.tution.app.data.PaymentEntity
import com.tution.app.data.StudentEntity
import com.tution.app.data.StudentStatusEvent
import com.tution.app.data.TeacherEntity
import com.tution.app.data.TeacherPaymentEntity
import com.tution.app.data.TutionDatabase
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.text.toBigDecimalOrNull

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = TutionDatabase.create(this)
        setContent { MaterialTheme(colorScheme = TutionColors) { TutionApp(database) } }
    }
}

private enum class Tab(val label: String) { Students("Students"), Dues("Dues"), Attendance("Attendance"), Teachers("Teachers"), History("History"), Settings("Settings") }

private val TutionColors = lightColorScheme(
    primary = Color(0xFF6750A4), onPrimary = Color.White,
    secondary = Color(0xFF006C67), tertiary = Color(0xFFB3265E),
    background = Color(0xFFFFF8FC), surface = Color(0xFFFFF8FC), surfaceVariant = Color(0xFFECE5F2)
)
private val studentColors = listOf(Color(0xFFE8DEF8), Color(0xFFD9E2FF), Color(0xFFD6F4ED), Color(0xFFFFE0B2))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TutionApp(database: TutionDatabase) {
    var tab by remember { mutableStateOf(Tab.Students) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var selectedTeacherId by remember { mutableStateOf<Long?>(null) }
    val activeStudents by database.dao().activeStudents().collectAsStateWithLifecycle(initialValue = emptyList())
    val allStudents by database.dao().allStudents().collectAsStateWithLifecycle(initialValue = emptyList())
    val allTeachers by database.dao().allTeachers().collectAsStateWithLifecycle(initialValue = emptyList())
    
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val selected = allStudents.firstOrNull { it.id == selectedId }
    if (selectedId != null && selected != null) {
        StudentDetail(selected, database) { selectedId = null }
        return
    }

    val selectedTeacher = allTeachers.firstOrNull { it.id == selectedTeacherId }
    if (selectedTeacherId != null && selectedTeacher != null) {
        TeacherDetail(selectedTeacher, database) { selectedTeacherId = null }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("Tution Manager", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Tab.entries.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = tab == item,
                        onClick = {
                            tab = item
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text(tab.label) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF6F0FA))
                )
            },
            floatingActionButton = { 
                if (tab == Tab.Students) AddStudentButton(database)
                else if (tab == Tab.Teachers) AddTeacherButton(database)
            }
        ) { padding ->
            when (tab) {
                Tab.Students -> StudentsScreen(activeStudents, database, { selectedId = it.id }, Modifier.padding(padding))
                Tab.Dues -> DuesScreen(activeStudents, allStudents, database, Modifier.padding(padding))
                Tab.Attendance -> AttendanceScreen(activeStudents, database, Modifier.padding(padding))
                Tab.Teachers -> TeachersScreen(allTeachers.filter { it.isActive }, { selectedTeacherId = it.id }, Modifier.padding(padding))
                Tab.History -> HistoryScreen(allStudents, { selectedId = it.id }, Modifier.padding(padding))
                Tab.Settings -> SettingsScreen(database, allStudents, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun AddTeacherButton(database: TutionDatabase) {
    var open by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    FloatingActionButton(onClick = { open = true }, containerColor = MaterialTheme.colorScheme.secondary, contentColor = Color.White, shape = RoundedCornerShape(18.dp)) { Text("+") }
    if (open) TeacherDialog(onDismiss = { open = false }) { teacher ->
        scope.launch { database.dao().addTeacher(teacher); open = false }
    }
}

@Composable
private fun AddStudentButton(database: TutionDatabase) {
    var open by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    FloatingActionButton(onClick = { open = true }, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White, shape = RoundedCornerShape(18.dp)) { Text("+") }
    if (open) StudentDialog(onDismiss = { open = false }) { student ->
        scope.launch { database.dao().addStudent(student); open = false }
    }
}

@Composable
private fun StudentsScreen(students: List<StudentEntity>, database: TutionDatabase, onClick: (StudentEntity) -> Unit, modifier: Modifier) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredStudents = remember(students, searchQuery) {
        students.filter { it.fullName.contains(searchQuery, ignoreCase = true) }
    }
    val feeChanges by database.dao().allFeeAdjustments().collectAsStateWithLifecycle(initialValue = emptyList())
    
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Your classroom", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("${students.size} active students enrolled", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Search by name...") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        if (students.isEmpty()) {
            EmptyScreen("Students", "Tap + to add your first student.", Modifier.weight(1f))
        } else if (filteredStudents.isEmpty()) {
            EmptyScreen("No results", "No students found matching \"$searchQuery\"", Modifier.weight(1f))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
                item { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE7DEF8)), shape = RoundedCornerShape(24.dp)) { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text("✦", style = MaterialTheme.typography.headlineMedium); Spacer(Modifier.width(12.dp)); Column { Text("Stay on top of fees", fontWeight = FontWeight.Bold); Text("Tap a student to see payments and due balance.", style = MaterialTheme.typography.bodySmall) } } } }
                items(filteredStudents, key = { it.id }) { student -> StudentCard(student, feeChanges, Modifier.clickable { onClick(student) }) }
            }
        }
    }
}

@Composable
private fun DuesScreen(students: List<StudentEntity>, allStudents: List<StudentEntity>, database: TutionDatabase, modifier: Modifier) {
    val context = LocalContext.current
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    val month = visibleMonth.toString()
    val allPayments by database.dao().allPayments().collectAsStateWithLifecycle(initialValue = emptyList())
    val monthPayments by database.dao().paymentsForMonth(month).collectAsStateWithLifecycle(initialValue = emptyList())
    val feeChanges by database.dao().allFeeAdjustments().collectAsStateWithLifecycle(initialValue = emptyList())
    val statusEvents by database.dao().allStatusEvents().collectAsStateWithLifecycle(initialValue = emptyList())

    val feeChangesByStudent = feeChanges.groupBy { it.studentId }
    val statusEventsByStudent = statusEvents.groupBy { it.studentId }
    val enrolledStudents = allStudents.filter { it.isExpectedFor(visibleMonth, statusEventsByStudent[it.id].orEmpty()) }
    val enrolledIds = enrolledStudents.map { it.id }.toSet()

    val collectedThisMonth = allPayments.filter { it.billingMonth == month && it.studentId in enrolledIds }.sumOf { it.amountPaise }
    val expectedThisMonth = enrolledStudents.sumOf { it.feeFor(visibleMonth, feeChangesByStudent[it.id].orEmpty()) }

    val archivedPaymentsThisMonth = allPayments.filter { it.billingMonth == month && it.studentId !in enrolledIds }.sumOf { it.amountPaise }

    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("Dues overview", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { visibleMonth = visibleMonth.minusMonths(1) }) { Text("‹ Previous") }; Text(month, fontWeight = FontWeight.Bold); TextButton(onClick = { visibleMonth = visibleMonth.plusMonths(1) }, enabled = visibleMonth < YearMonth.now()) { Text("Next ›") } } }
        item { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFD8F2EA)), shape = RoundedCornerShape(24.dp)) { Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("Current students", color = Color(0xFF14655A), fontWeight = FontWeight.Bold)
            Text("${rupees(collectedThisMonth)} received", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${rupees(expectedThisMonth)} expected for $month", color = Color(0xFF255B53))
            if (archivedPaymentsThisMonth > 0) Text("${rupees(archivedPaymentsThisMonth)} received from archived students", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall, color = Color(0xFF255B53))
        } } }
        item { Text("Student balances", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(enrolledStudents.sortedBy { student ->
            val totalExpected = student.expectedUpTo(visibleMonth, feeChangesByStudent[student.id].orEmpty(), statusEventsByStudent[student.id].orEmpty())
            val totalPaid = student.paidUpTo(visibleMonth, allPayments)
            totalPaid - totalExpected
        }, key = { "monthly-student-${it.id}" }) { student ->
            val changes = feeChangesByStudent[student.id].orEmpty()
            val events = statusEventsByStudent[student.id].orEmpty()
            val monthlyFee = student.feeFor(visibleMonth, changes)
            val totalExpected = student.expectedUpTo(visibleMonth, changes, events)
            val totalPaid = student.paidUpTo(visibleMonth, allPayments)
            val balance = totalPaid - totalExpected

            val prevMonth = visibleMonth.minusMonths(1)
            val prevExpected = student.expectedUpTo(prevMonth, changes, events)
            val prevPaid = student.paidUpTo(prevMonth, allPayments)
            val carryForward = prevPaid - prevExpected

            val color = when { balance >= 0L -> Color(0xFF1B7F3A); balance < 0L && balance > -monthlyFee -> Color(0xFF8A5700); else -> Color(0xFFB3261E) }
            val hasFeeChange = changes.any { it.effectiveFrom == month }

            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(student); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(student.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Due on the ${student.dueDay}th", style = MaterialTheme.typography.bodySmall)
                    }
                    if (hasFeeChange) Surface(color = Color(0xFFE7DEF8), shape = RoundedCornerShape(8.dp)) { Text("Fee updated", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall) }
                }
                Spacer(Modifier.height(12.dp))
                if (carryForward != 0L) {
                    Text(if (carryForward > 0) "Advance b/f: ${rupees(carryForward)}" else "Arrears b/f: ${rupees(-carryForward)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Fee ${rupees(monthlyFee)} · Paid ${rupees(totalPaid - prevPaid)}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = when {
                        balance > 0L -> "Advance ${rupees(balance)}"
                        balance == 0L -> "✓ Paid in full"
                        else -> "Due ${rupees(-balance)}"
                    },
                    color = color,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (balance < 0L) {
                    TextButton(onClick = {
                        val msg = "Hello! Tuition fee reminder for ${student.fullName}.\nDue for $visibleMonth: ${rupees(monthlyFee)}\nTotal Dues: ${rupees(-balance)}\nPlease ignore if already paid. Thanks!"
                        sendWhatsAppReminder(context, student.phone, msg)
                    }, modifier = Modifier.align(Alignment.End)) { Text("Send WhatsApp Reminder") }
                }
            } }
        }
        item { Text("Payments received", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (monthPayments.isEmpty()) item { Text("No payments recorded for this month.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(monthPayments, key = { "monthly-payment-${it.id}" }) { payment ->
            val studentName = allStudents.firstOrNull { it.id == payment.studentId }?.fullName ?: "Former student"
            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) { 
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(studentName, fontWeight = FontWeight.Bold)
                            Text("Paid ${payment.paidOn} · ${payment.method}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(rupees(payment.amountPaise), color = Color(0xFF1B7F3A), fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = {
                        val msg = "Hi! Received payment of ${rupees(payment.amountPaise)} for $studentName (Billing Month: ${payment.billingMonth}) on ${payment.paidOn}. Thank you!"
                        val phone = allStudents.find { it.id == payment.studentId }?.phone ?: ""
                        sendWhatsAppReminder(context, phone, msg)
                    }, modifier = Modifier.align(Alignment.End)) { Text("Send Receipt (WhatsApp)") }
                }
            }
        }
    }
}

@Composable
private fun AttendanceScreen(students: List<StudentEntity>, database: TutionDatabase, modifier: Modifier) {
    var date by remember { mutableStateOf(LocalDate.now()) }
    val attendance by database.dao().attendanceForDate(date.toString()).collectAsStateWithLifecycle(initialValue = emptyList())
    val attendanceMap = attendance.associate { it.studentId to it.isPresent }
    val scope = rememberCoroutineScope()
    
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Attendance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { date = date.minusDays(1) }) { Text("‹ Previous") }
            Text(date.toString(), fontWeight = FontWeight.Bold)
            TextButton(onClick = { date = date.plusDays(1) }, enabled = date < LocalDate.now()) { Text("Next ›") }
        }
        
        if (students.isEmpty()) {
            EmptyScreen("No students", "Add students to track attendance.", Modifier.weight(1f))
        } else {
            val enrolledOnDate = remember(students, date) {
                students.filter { LocalDate.parse(it.joinedOn) <= date }
            }
            if (enrolledOnDate.isEmpty()) {
                EmptyScreen("No students", "No students were enrolled on this date.", Modifier.weight(1f))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(enrolledOnDate, key = { it.id }) { student ->
                        val isPresent = attendanceMap[student.id] ?: false
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (isPresent) Color(0xFFD8F2EA) else Color.White),
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                scope.launch {
                                    database.dao().markAttendance(AttendanceEntity(studentId = student.id, date = date.toString(), isPresent = !isPresent))
                                }
                            }
                        ) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Avatar(student)
                                Spacer(Modifier.width(12.dp))
                                Text(student.fullName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                Text(if (isPresent) "✓ Present" else "○ Absent", color = if (isPresent) Color(0xFF1B7F3A) else Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeachersScreen(teachers: List<TeacherEntity>, onClick: (TeacherEntity) -> Unit, modifier: Modifier) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(teachers, searchQuery) {
        teachers.filter { it.fullName.contains(searchQuery, ignoreCase = true) || it.subject.contains(searchQuery, ignoreCase = true) }
    }
    Column(modifier.fillMaxSize().padding(20.dp)) {
        Text("Teachers", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("${teachers.size} faculty members", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text("Search by name or subject...") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        if (teachers.isEmpty()) EmptyScreen("Teachers", "Tap + to add your first faculty member.", Modifier.weight(1f))
        else if (filtered.isEmpty()) EmptyScreen("No results", "No teachers found matching \"$searchQuery\"", Modifier.weight(1f))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.id }) { teacher ->
                Card(onClick = { onClick(teacher) }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AvatarForTeacher(teacher)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(teacher.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(teacher.subject, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(rupees(teacher.monthlySalaryPaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Text("salary", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherDetail(teacher: TeacherEntity, database: TutionDatabase, onBack: () -> Unit) {
    val payments by database.dao().teacherPaymentsFor(teacher.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var paymentOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    var confirmUndo by remember { mutableStateOf<TeacherPaymentEntity?>(null) }
    val month = YearMonth.now().toString()
    val paid = payments.filter { it.billingMonth == month }.sumOf { it.amountPaise }
    val remaining = max(0L, teacher.monthlySalaryPaise - paid)

    Scaffold { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { TextButton(onClick = onBack) { Text("‹ Teachers") } }
            item { Text(teacher.fullName, style = MaterialTheme.typography.headlineMedium) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F0F4)), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Salary Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Text("Monthly Salary: ${rupees(teacher.monthlySalaryPaise)}")
                        Text("Paid for $month: ${rupees(paid)}")
                        Text("Remaining for $month: ${rupees(remaining)}", color = if (remaining == 0L) Color(0xFF1B7F3A) else Color(0xFFB3261E))
                    }
                }
            }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { paymentOpen = true }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("Pay Salary") }
                OutlinedButton(onClick = { editOpen = true }, modifier = Modifier.weight(1f)) { Text("Edit Info") }
            } }
            if (payments.isNotEmpty()) item { TextButton(onClick = { confirmUndo = payments.first() }) { Text("Undo latest payment") } }
            item { Text("Payment history", style = MaterialTheme.typography.titleLarge) }
            if (payments.isEmpty()) item { Text("No payments recorded.") }
            items(payments, key = { "t-payment-${it.id}" }) { p ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Month: ${p.billingMonth}", fontWeight = FontWeight.Bold)
                            Text("Paid ${p.paidOn} · ${p.method}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(rupees(p.amountPaise), color = Color(0xFF1B7F3A), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    if (paymentOpen) TeacherPaymentDialog(teacher, onDismiss = { paymentOpen = false }) { p -> scope.launch { database.dao().addTeacherPayment(p); paymentOpen = false } }
    if (editOpen) TeacherDialog(teacher = teacher, onDismiss = { editOpen = false }) { updated -> scope.launch { database.dao().updateTeacher(updated); editOpen = false } }
    confirmUndo?.let { p ->
        AlertDialog(
            onDismissRequest = { confirmUndo = null }, title = { Text("Undo payment?") },
            text = { Text("Remove ${rupees(p.amountPaise)} paid for ${p.billingMonth}?") },
            confirmButton = { Button(onClick = { scope.launch { database.dao().deleteTeacherPayment(p); confirmUndo = null } }) { Text("Undo payment") } },
            dismissButton = { TextButton(onClick = { confirmUndo = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun TeacherDialog(teacher: TeacherEntity? = null, onDismiss: () -> Unit, onSave: (TeacherEntity) -> Unit) {
    var name by remember { mutableStateOf(teacher?.fullName ?: "") }; var phone by remember { mutableStateOf(teacher?.phone ?: "") }
    var salary by remember { mutableStateOf(teacher?.let { (it.monthlySalaryPaise / 100.0).toString() } ?: "") }
    var subject by remember { mutableStateOf(teacher?.subject ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (teacher == null) "Add teacher" else "Edit teacher") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Field("Full name", name) { name = it }; Field("Phone", phone) { phone = it }
        Field("Subject", subject) { subject = it }; Field("Monthly Salary (₹)", salary) { salary = it }
    } }, confirmButton = { Button(onClick = {
        val salaryPaise = toPaise(salary)
        if (name.isNotBlank() && salaryPaise != null && salaryPaise > 0) {
            val updated = teacher?.copy(fullName = name.trim(), phone = phone.trim(), subject = subject.trim(), monthlySalaryPaise = salaryPaise)
                ?: TeacherEntity(fullName = name.trim(), phone = phone.trim(), subject = subject.trim(), monthlySalaryPaise = salaryPaise, joinedOn = LocalDate.now().toString())
            onSave(updated)
        }
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun TeacherPaymentDialog(teacher: TeacherEntity, onDismiss: () -> Unit, onSave: (TeacherPaymentEntity) -> Unit) {
    var amount by remember { mutableStateOf("") }; var billingMonth by remember { mutableStateOf(YearMonth.now().toString()) }
    var method by remember { mutableStateOf("Cash") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Pay teacher") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Field("Amount (₹)", amount) { amount = it }; Field("For Month (YYYY-MM)", billingMonth) { billingMonth = it }
        PaymentMethodDropdown(method) { method = it }
    } }, confirmButton = { Button(onClick = {
        val paise = toPaise(amount)
        if (paise != null && paise > 0 && billingMonth.toYearMonthOrNull() != null) {
            onSave(TeacherPaymentEntity(teacherId = teacher.id, billingMonth = billingMonth, paidOn = LocalDate.now().toString(), amountPaise = paise, method = method))
        }
    }) { Text("Confirm Payment") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun AvatarForTeacher(teacher: TeacherEntity) { val color = studentColors[(teacher.id % studentColors.size.toLong()).toInt()]; Box(Modifier.size(48.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { Text(teacher.fullName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF42375B)) } }

@Composable
private fun HistoryScreen(students: List<StudentEntity>, onClick: (StudentEntity) -> Unit, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Column { Text("Student history", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Every learner, including archived records", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        items(students, key = { it.id }) { student -> Card(Modifier.clickable { onClick(student) }, colors = CardDefaults.cardColors(containerColor = if (student.isActive) Color.White else Color(0xFFF1EDF3)), shape = RoundedCornerShape(22.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(student); Spacer(Modifier.width(12.dp)); Column {
                Text(student.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(if (student.isActive) "● Active" else "Archived on ${student.leftOn}", color = if (student.isActive) Color(0xFF1B7F3A) else MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Joined ${student.joinedOn} · ${rupees(student.monthlyFeePaise)}", style = MaterialTheme.typography.bodySmall)
            }
        } } }
    }
}

@Composable
private fun SettingsScreen(database: TutionDatabase, allStudents: List<StudentEntity>, modifier: Modifier) = Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmClear by remember { mutableStateOf(false) }

    Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    SettingsCard("🔔", "Fee reminders", "Create due-date and partial-fee reminders", Color(0xFFE7DEF8)) {}
    SettingsCard("↗", "Backup & export", "Share tuition records as CSV", Color(0xFFD8F2EA)) {
        scope.launch {
            val payments = database.dao().allPayments().first()
            exportDataToCSV(context, allStudents, payments)
        }
    }
    SettingsCard("✉", "Message templates", "Prepare parent reminders before sending", Color(0xFFFFE4C1)) {}
    SettingsCard("🗑️", "Clear all data", "Permanently delete all records", Color(0xFFFFDAD6)) {
        confirmClear = true
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Wipe all data?") },
            text = { Text("This will permanently delete all students, teachers, payments, and attendance records. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                // Delete dependent records first (Foreign Key compliance)
                                database.dao().clearPayments()
                                database.dao().clearFeeAdjustments()
                                database.dao().clearAttendance()
                                database.dao().clearStatusEvents()
                                database.dao().clearTeacherPayments()
                                // Delete main records last
                                database.dao().clearStudents()
                                database.dao().clearTeachers()
                            }
                            confirmClear = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                ) { Text("Delete Everything") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsCard(icon: String, title: String, subtitle: String, color: Color, onClick: () -> Unit) = Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = color), shape = RoundedCornerShape(22.dp)) { Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.width(14.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall) } } }

@Composable
private fun StudentDetail(student: StudentEntity, database: TutionDatabase, onBack: () -> Unit) {
    val context = LocalContext.current
    val payments by database.dao().paymentsFor(student.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val feeChanges by database.dao().feeAdjustmentsFor(student.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val statusEvents by database.dao().allStatusEvents().collectAsStateWithLifecycle(initialValue = emptyList())
    val attendance by database.dao().attendanceForStudent(student.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val studentEvents = statusEvents.filter { it.studentId == student.id }
    val scope = rememberCoroutineScope()
    var paymentOpen by remember { mutableStateOf(false) }
    var feeOpen by remember { mutableStateOf(false) }
    var editOpen by remember { mutableStateOf(false) }
    var dueDayOpen by remember { mutableStateOf(false) }
    var confirmFullPayment by remember { mutableStateOf(false) }
    var confirmUndo by remember { mutableStateOf<PaymentEntity?>(null) }
    var confirmRejoin by remember { mutableStateOf(false) }
    var fullPaymentMethod by remember { mutableStateOf("Cash") }
    val month = YearMonth.now().toString()
    val paid = payments.filter { it.billingMonth == month }.sumOf { it.amountPaise }
    val monthFee = student.feeFor(YearMonth.now(), feeChanges)
    val remaining = max(0L, monthFee - paid)

    val currentMonth = YearMonth.now()
    val monthAttendance = attendance.filter { YearMonth.from(LocalDate.parse(it.date)) == currentMonth }
    val presentCount = monthAttendance.count { it.isPresent }
    val totalRecorded = monthAttendance.size
    Scaffold { padding -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = onBack) { Text("‹ Students") } }
        item { Text(student.fullName, style = MaterialTheme.typography.headlineMedium) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F0F4)), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Monthly Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Due: ${student.dueDay}th · Fee: ${rupees(monthFee)}")
                    Text("Paid: ${rupees(paid)} · Remaining: ${rupees(remaining)}", color = if (remaining == 0L) Color(0xFF1B7F3A) else Color(0xFFB3261E))
                    Spacer(Modifier.height(8.dp))
                    Text("Attendance: ${if (totalRecorded > 0) "$presentCount / $totalRecorded days present" else "No records yet"}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { paymentOpen = true }, modifier = Modifier.weight(1f)) { Text("Record payment") }
            Button(onClick = { feeOpen = true }, modifier = Modifier.weight(1f)) { Text("Change fee") }
        } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                val msg = "Hello! Tuition fee reminder for ${student.fullName}.\nPending balance: ${rupees(remaining)}\nPlease ignore if already paid. Thanks!"
                sendWhatsAppReminder(context, student.phone, msg)
            }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))) { Text("WhatsApp Reminder") }
            OutlinedButton(onClick = { editOpen = true }, modifier = Modifier.weight(1f)) { Text("Edit Info") }
        } }
        if (student.isActive) item { TextButton(onClick = { dueDayOpen = true }) { Text("Change due date") } }
        if (remaining > 0) item { Button(onClick = { confirmFullPayment = true }) { Text("Mark fully paid (${rupees(remaining)})") } }
        if (payments.isNotEmpty()) item { TextButton(onClick = { scope.launch { confirmUndo = payments.first(); } }) { Text("Undo latest payment") } }
        if (student.isActive) item { TextButton(onClick = { scope.launch { val date = LocalDate.now().toString(); database.dao().archiveStudent(student.id, date); database.dao().addStatusEvent(StudentStatusEvent(studentId = student.id, changedOn = date, status = "ARCHIVED")); onBack() } }) { Text("Archive student") } }
        else item { Button(onClick = { confirmRejoin = true }) { Text("Rejoin student") } }
        item { Text("Fee change history", style = MaterialTheme.typography.titleLarge) }
        if (feeChanges.isEmpty()) item { Text("No fee changes recorded.") }
        items(feeChanges, key = { "fee-change-${it.id}" }) { change -> Text("${change.effectiveFrom}: ${rupees(change.oldFeePaise)} → ${rupees(change.newFeePaise)}") }
        item { Text("Payment history", style = MaterialTheme.typography.titleLarge) }
        if (payments.isEmpty()) item { Text("No payments recorded.") }
        items(payments, key = { "payment-${it.id}" }) { payment -> Text("${payment.paidOn} · ${payment.billingMonth} · ${rupees(payment.amountPaise)} · ${payment.method}") }
    } }
    if (paymentOpen) PaymentDialog(student, payments, feeChanges, studentEvents, onDismiss = { paymentOpen = false }) { payment -> scope.launch { database.dao().addPayment(payment); paymentOpen = false } }
    if (feeOpen) FeeDialog(student, onDismiss = { feeOpen = false }) { adjustment -> scope.launch { database.dao().addFeeAdjustment(adjustment); database.dao().updateCurrentFee(student.id, adjustment.newFeePaise); feeOpen = false } }
    if (dueDayOpen) DueDayDialog(student, onDismiss = { dueDayOpen = false }) { dueDay -> scope.launch { database.dao().updateDueDay(student.id, dueDay); dueDayOpen = false } }
    if (confirmFullPayment) AlertDialog(
        onDismissRequest = { confirmFullPayment = false }, title = { Text("Mark fully paid?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Record ${rupees(remaining)} for $month?"); PaymentMethodDropdown(fullPaymentMethod) { fullPaymentMethod = it } } },
        confirmButton = { Button(onClick = { scope.launch { database.dao().addPayment(PaymentEntity(studentId = student.id, billingMonth = month, paidOn = LocalDate.now().toString(), amountPaise = remaining, method = fullPaymentMethod)); confirmFullPayment = false } }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = { confirmFullPayment = false }) { Text("Cancel") } }
    )
    confirmUndo?.let { payment -> AlertDialog(
        onDismissRequest = { confirmUndo = null }, title = { Text("Undo latest payment?") },
        text = { Text("Remove ${rupees(payment.amountPaise)} recorded on ${payment.paidOn}? This cannot be undone.") },
        confirmButton = { Button(onClick = { scope.launch { database.dao().deletePayment(payment); confirmUndo = null } }) { Text("Undo payment") } },
        dismissButton = { TextButton(onClick = { confirmUndo = null }) { Text("Cancel") } }
    ) }
    if (confirmRejoin) AlertDialog(
        onDismissRequest = { confirmRejoin = false }, title = { Text("Rejoin student?") },
        text = { Text("This restores ${student.fullName} to the active list. Their existing payment and fee history stays unchanged.") },
        confirmButton = { Button(onClick = { scope.launch { val date = LocalDate.now().toString(); database.dao().rejoinStudent(student.id); database.dao().addStatusEvent(StudentStatusEvent(studentId = student.id, changedOn = date, status = "REJOINED")); confirmRejoin = false } }) { Text("Rejoin") } },
        dismissButton = { TextButton(onClick = { confirmRejoin = false }) { Text("Cancel") } }
    )
    if (editOpen) StudentDialog(student = student, onDismiss = { editOpen = false }) { updated -> scope.launch { database.dao().updateStudent(updated); editOpen = false } }
}

@Composable
private fun StudentDialog(student: StudentEntity? = null, onDismiss: () -> Unit, onSave: (StudentEntity) -> Unit) {
    var name by remember { mutableStateOf(student?.fullName ?: "") }; var phone by remember { mutableStateOf(student?.phone ?: "") }
    var fee by remember { mutableStateOf(student?.let { (it.monthlyFeePaise / 100.0).toString() } ?: "") }; var dueDay by remember { mutableStateOf(student?.dueDay?.toString() ?: "5") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (student == null) "Add student" else "Edit student") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Field("Student name", name) { name = it }; Field("Guardian phone", phone) { phone = it }
        Field("Monthly fee (₹)", fee) { fee = it }; Field("Due day (1–31)", dueDay) { dueDay = it }
    } }, confirmButton = { Button(onClick = {
        val feePaise = toPaise(fee); val day = dueDay.toIntOrNull()
        if (name.isNotBlank() && feePaise != null && feePaise > 0 && day != null && day in 1..31) {
            val updated = student?.copy(fullName = name.trim(), phone = phone.trim(), monthlyFeePaise = feePaise, dueDay = day) 
                ?: StudentEntity(fullName = name.trim(), phone = phone.trim(), joinedOn = LocalDate.now().toString(), monthlyFeePaise = feePaise, dueDay = day)
            onSave(updated)
        }
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun PaymentDialog(student: StudentEntity, payments: List<PaymentEntity>, feeChanges: List<FeeAdjustmentEntity>, statusEvents: List<StudentStatusEvent>, onDismiss: () -> Unit, onSave: (PaymentEntity) -> Unit) {
    var amount by remember { mutableStateOf("") }; var billingMonth by remember { mutableStateOf(YearMonth.now().toString()) }
    var method by remember { mutableStateOf("Cash") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Record payment") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val selectedMonth = billingMonth.toYearMonthOrNull()
        if (selectedMonth != null) {
            val totalExpected = student.expectedUpTo(selectedMonth, feeChanges, statusEvents)
            val totalPaid = student.paidUpTo(selectedMonth, payments)
            val balance = totalPaid - totalExpected
            Text(if (balance < 0) "Pending dues: ${rupees(-balance)}" else "Current advance: ${rupees(balance)}", color = if (balance < 0) Color(0xFFB3261E) else Color(0xFF1B7F3A), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Field("Amount paid (₹)", amount) { amount = it; error = null }; Field("Billing month (YYYY-MM)", billingMonth) { billingMonth = it; error = null }; PaymentMethodDropdown(method) { method = it }
        error?.let { Text(it, color = Color(0xFFB3261E)) }
    } }, confirmButton = { Button(onClick = {
        val paise = toPaise(amount)
        when {
            paise == null || paise <= 0 -> error = "Enter a valid payment amount."
            billingMonth.toYearMonthOrNull() == null -> error = "Use a valid billing month, for example 2026-08."
            else -> onSave(PaymentEntity(studentId = student.id, billingMonth = billingMonth, paidOn = LocalDate.now().toString(), amountPaise = paise, method = method))
        }
    }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun PaymentMethodDropdown(value: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("Payment method", style = MaterialTheme.typography.labelLarge)
        Box {
            Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(value) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("Cash", "UPI").forEach { method -> DropdownMenuItem(text = { Text(method) }, onClick = { onSelect(method); expanded = false }) }
            }
        }
    }
}

@Composable
private fun FeeDialog(student: StudentEntity, onDismiss: () -> Unit, onSave: (FeeAdjustmentEntity) -> Unit) {
    var fee by remember { mutableStateOf("") }
    var effectiveFrom by remember { mutableStateOf(YearMonth.now().plusMonths(1).toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Change monthly fee") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Current monthly fee: ${rupees(student.monthlyFeePaise)}", style = MaterialTheme.typography.bodyMedium)
        Field("New fee (₹)", fee) { fee = it; error = null }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val thisMonth = YearMonth.now().toString()
            val nextMonth = YearMonth.now().plusMonths(1).toString()
            FilterChip(selected = effectiveFrom == thisMonth, onClick = { effectiveFrom = thisMonth }, label = { Text("From this month") })
            FilterChip(selected = effectiveFrom == nextMonth, onClick = { effectiveFrom = nextMonth }, label = { Text("From next month") })
        }
        Field("Or specific month (YYYY-MM)", effectiveFrom) { effectiveFrom = it; error = null }
        error?.let { Text(it, color = Color(0xFFB3261E)) }
    } }, confirmButton = { Button(onClick = { val newFee = toPaise(fee); if (newFee == null || newFee <= 0) error = "Enter a valid fee." else if (effectiveFrom.toYearMonthOrNull() == null) error = "Use a valid month, for example 2026-09." else onSave(FeeAdjustmentEntity(studentId = student.id, effectiveFrom = effectiveFrom, oldFeePaise = student.monthlyFeePaise, newFeePaise = newFee)) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun DueDayDialog(student: StudentEntity, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var dayText by remember { mutableStateOf(student.dueDay.toString()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change due date") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Choose the day of every month when this fee is due.")
            Field("Due day (1–31)", dayText) { dayText = it; error = null }
            error?.let { Text(it, color = Color(0xFFB3261E)) }
        } },
        confirmButton = { Button(onClick = { val day = dayText.toIntOrNull(); if (day != null && day in 1..31) onSave(day) else error = "Enter a day from 1 to 31." }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable private fun Field(label: String, value: String, onValueChange: (String) -> Unit) = OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
@Composable private fun StudentCard(student: StudentEntity, allChanges: List<FeeAdjustmentEntity>, modifier: Modifier) = Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(22.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
    Avatar(student); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
        Text(student.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Due on the ${student.dueDay}th", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val futureChange = allChanges.filter { it.studentId == student.id && YearMonth.parse(it.effectiveFrom) > YearMonth.now() }.minByOrNull { it.effectiveFrom }
        if (futureChange != null) Text("Updating to ${rupees(futureChange.newFeePaise)} in ${futureChange.effectiveFrom}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }; Column(horizontalAlignment = Alignment.End) { Text(rupees(student.monthlyFeePaise), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Text("monthly", style = MaterialTheme.typography.labelSmall) }; Spacer(Modifier.width(8.dp)); Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
} }
@Composable private fun Avatar(student: StudentEntity) { val color = studentColors[(student.id % studentColors.size.toLong()).toInt()]; Box(Modifier.size(48.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { Text(student.fullName.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF42375B)) } }
@Composable private fun EmptyScreen(title: String, message: String, modifier: Modifier) = Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("✦", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(message) }
private fun toPaise(value: String): Long? = value.replace(",", "").toBigDecimalOrNull()?.movePointRight(2)?.toLong()
private fun rupees(paise: Long): String = "₹%.2f".format(paise / 100.0)
private fun String.toYearMonthOrNull(): YearMonth? = runCatching { YearMonth.parse(this) }.getOrNull()

private fun exportDataToCSV(context: Context, students: List<StudentEntity>, payments: List<PaymentEntity>) {
    val csv = StringBuilder().apply {
        append("Student,Month,Paid On,Amount,Method\n")
        payments.forEach { p ->
            val name = students.find { it.id == p.studentId }?.fullName ?: "Unknown"
            append("\"$name\",${p.billingMonth},${p.paidOn},${p.amountPaise / 100.0},${p.method}\n")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Tuition Backup ${LocalDate.now()}")
        putExtra(Intent.EXTRA_TEXT, csv.toString())
    }
    context.startActivity(Intent.createChooser(intent, "Share Backup"))
}

private fun sendWhatsAppReminder(context: Context, phone: String, message: String) {
    val cleanPhone = phone.filter { it.isDigit() }
    val formattedPhone = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
    }
    context.startActivity(intent)
}

private fun StudentEntity.expectedUpTo(targetMonth: YearMonth, changes: List<FeeAdjustmentEntity>, events: List<StudentStatusEvent>): Long {
    val joinedMonth = YearMonth.from(LocalDate.parse(joinedOn))
    var totalExpected = 0L
    var currentMonth = joinedMonth
    while (currentMonth <= targetMonth) {
        if (isExpectedFor(currentMonth, events)) {
            totalExpected += feeFor(currentMonth, changes)
        }
        currentMonth = currentMonth.plusMonths(1)
    }
    return totalExpected
}
private fun StudentEntity.paidUpTo(targetMonth: YearMonth, allPayments: List<PaymentEntity>): Long {
    return allPayments.filter { it.studentId == this.id && YearMonth.parse(it.billingMonth) <= targetMonth }.sumOf { it.amountPaise }
}
private fun StudentEntity.isExpectedFor(month: YearMonth, events: List<StudentStatusEvent>): Boolean {
    val joinedMonth = YearMonth.from(LocalDate.parse(joinedOn))
    if (joinedMonth > month) return false
    return events.filter { YearMonth.from(LocalDate.parse(it.changedOn)) <= month }.sortedWith(compareBy({ it.changedOn }, { it.id })).fold(true) { active, event -> event.status == "REJOINED" }
}
private fun StudentEntity.feeFor(month: YearMonth, changes: List<FeeAdjustmentEntity>): Long {
    val onOrBefore = changes.filter { YearMonth.parse(it.effectiveFrom) <= month }.maxByOrNull { it.effectiveFrom }
    if (onOrBefore != null) return onOrBefore.newFeePaise
    return changes.minByOrNull { it.effectiveFrom }?.oldFeePaise ?: monthlyFeePaise
}
