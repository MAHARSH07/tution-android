package com.tution.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val guardianName: String = "",
    val phone: String = "",
    val joinedOn: String,
    val leftOn: String? = null,
    val isActive: Boolean = true,
    val monthlyFeePaise: Long,
    val dueDay: Int
)

/** Every fee change is retained; never overwrite a student's previous fee. */
@Entity(tableName = "fee_adjustments", foreignKeys = [ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("studentId")])
data class FeeAdjustmentEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val studentId: Long, val effectiveFrom: String, val oldFeePaise: Long, val newFeePaise: Long, val note: String = "")

/** A payment is immutable ledger data. Use a correction record instead of altering a payment. */
@Entity(tableName = "payments", foreignKeys = [ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("studentId"), Index("billingMonth")])
data class PaymentEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val studentId: Long, val billingMonth: String, val paidOn: String, val amountPaise: Long, val method: String = "Cash", val note: String = "")

@Entity(tableName = "attendance", foreignKeys = [ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("studentId"), Index("date"), Index(value = ["studentId", "date"], unique = true)])
data class AttendanceEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val studentId: Long, val date: String, val isPresent: Boolean)

data class MonthPaymentTotal(val studentId: Long, val paidPaise: Long)

@Entity(tableName = "student_status_events", foreignKeys = [ForeignKey(entity = StudentEntity::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("studentId")])
data class StudentStatusEvent(@PrimaryKey(autoGenerate = true) val id: Long = 0, val studentId: Long, val changedOn: String, val status: String)

@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val phone: String = "",
    val subject: String = "",
    val monthlySalaryPaise: Long,
    val joinedOn: String,
    val isActive: Boolean = true
)

@Entity(tableName = "teacher_payments", foreignKeys = [ForeignKey(entity = TeacherEntity::class, parentColumns = ["id"], childColumns = ["teacherId"], onDelete = ForeignKey.RESTRICT)], indices = [Index("teacherId")])
data class TeacherPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teacherId: Long,
    val billingMonth: String,
    val paidOn: String,
    val amountPaise: Long,
    val method: String = "Cash"
)

@Dao
interface TutionDao {
    @Query("SELECT * FROM students WHERE isActive = 1 ORDER BY fullName COLLATE NOCASE") fun activeStudents(): Flow<List<StudentEntity>>
    @Query("SELECT * FROM students ORDER BY isActive DESC, fullName COLLATE NOCASE") fun allStudents(): Flow<List<StudentEntity>>
    @Query("SELECT * FROM payments WHERE studentId = :studentId ORDER BY paidOn DESC, id DESC") fun paymentsFor(studentId: Long): Flow<List<PaymentEntity>>
    @Query("SELECT * FROM payments WHERE billingMonth = :billingMonth ORDER BY paidOn DESC, id DESC") fun paymentsForMonth(billingMonth: String): Flow<List<PaymentEntity>>
    @Query("SELECT * FROM fee_adjustments WHERE studentId = :studentId ORDER BY effectiveFrom DESC, id DESC") fun feeAdjustmentsFor(studentId: Long): Flow<List<FeeAdjustmentEntity>>
    @Query("SELECT * FROM fee_adjustments ORDER BY effectiveFrom ASC, id ASC") fun allFeeAdjustments(): Flow<List<FeeAdjustmentEntity>>
    @Query("SELECT * FROM student_status_events ORDER BY changedOn ASC, id ASC") fun allStatusEvents(): Flow<List<StudentStatusEvent>>
    @Query("SELECT * FROM payments ORDER BY billingMonth ASC, paidOn ASC") fun allPayments(): Flow<List<PaymentEntity>>
    @Query("SELECT studentId, COALESCE(SUM(amountPaise), 0) AS paidPaise FROM payments WHERE billingMonth = :billingMonth GROUP BY studentId") fun paymentTotalsForMonth(billingMonth: String): Flow<List<MonthPaymentTotal>>
    @Query("SELECT * FROM attendance WHERE date = :date") fun attendanceForDate(date: String): Flow<List<AttendanceEntity>>
    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date DESC") fun attendanceForStudent(studentId: Long): Flow<List<AttendanceEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun markAttendance(attendance: AttendanceEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun addStudent(student: StudentEntity): Long
    @Insert suspend fun addPayment(payment: PaymentEntity): Long
    @Delete suspend fun deletePayment(payment: PaymentEntity)
    @Insert suspend fun addFeeAdjustment(adjustment: FeeAdjustmentEntity): Long
    @Insert suspend fun addStatusEvent(event: StudentStatusEvent): Long
    @Query("UPDATE students SET isActive = 0, leftOn = :leftOn WHERE id = :studentId") suspend fun archiveStudent(studentId: Long, leftOn: String)
    @Query("UPDATE students SET isActive = 1, leftOn = NULL WHERE id = :studentId") suspend fun rejoinStudent(studentId: Long)
    @androidx.room.Update suspend fun updateStudent(student: StudentEntity)
    @Query("UPDATE students SET monthlyFeePaise = :newFeePaise WHERE id = :studentId") suspend fun updateCurrentFee(studentId: Long, newFeePaise: Long)
    @Query("UPDATE students SET dueDay = :dueDay WHERE id = :studentId") suspend fun updateDueDay(studentId: Long, dueDay: Int)

    @Query("SELECT * FROM teachers WHERE isActive = 1 ORDER BY fullName COLLATE NOCASE") fun activeTeachers(): Flow<List<TeacherEntity>>
    @Query("SELECT * FROM teachers ORDER BY isActive DESC, fullName COLLATE NOCASE") fun allTeachers(): Flow<List<TeacherEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun addTeacher(teacher: TeacherEntity): Long
    @androidx.room.Update suspend fun updateTeacher(teacher: TeacherEntity)
    @Query("SELECT * FROM teacher_payments WHERE teacherId = :teacherId ORDER BY paidOn DESC, id DESC") fun teacherPaymentsFor(teacherId: Long): Flow<List<TeacherPaymentEntity>>
    @Insert suspend fun addTeacherPayment(payment: TeacherPaymentEntity): Long
    @Delete suspend fun deleteTeacherPayment(payment: TeacherPaymentEntity)

    @Query("DELETE FROM students") suspend fun clearStudents()
    @Query("DELETE FROM payments") suspend fun clearPayments()
    @Query("DELETE FROM fee_adjustments") suspend fun clearFeeAdjustments()
    @Query("DELETE FROM attendance") suspend fun clearAttendance()
    @Query("DELETE FROM student_status_events") suspend fun clearStatusEvents()
    @Query("DELETE FROM teachers") suspend fun clearTeachers()
    @Query("DELETE FROM teacher_payments") suspend fun clearTeacherPayments()
}

@Database(entities = [StudentEntity::class, FeeAdjustmentEntity::class, PaymentEntity::class, StudentStatusEvent::class, AttendanceEntity::class, TeacherEntity::class, TeacherPaymentEntity::class], version = 5, exportSchema = false)
abstract class TutionDatabase : RoomDatabase() {
    abstract fun dao(): TutionDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS student_status_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, studentId INTEGER NOT NULL, changedOn TEXT NOT NULL, status TEXT NOT NULL, FOREIGN KEY(studentId) REFERENCES students(id) ON DELETE RESTRICT)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_student_status_events_studentId ON student_status_events(studentId)")
                database.execSQL("INSERT INTO student_status_events (studentId, changedOn, status) SELECT id, leftOn, 'ARCHIVED' FROM students WHERE leftOn IS NOT NULL")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS attendance (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, studentId INTEGER NOT NULL, date TEXT NOT NULL, isPresent INTEGER NOT NULL, FOREIGN KEY(studentId) REFERENCES students(id) ON DELETE RESTRICT)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_studentId ON attendance(studentId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_date ON attendance(date)")
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop duplicates before adding unique index
                database.execSQL("DELETE FROM attendance WHERE id NOT IN (SELECT MAX(id) FROM attendance GROUP BY studentId, date)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_attendance_studentId_date ON attendance(studentId, date)")
            }
        }
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS teachers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, fullName TEXT NOT NULL, phone TEXT NOT NULL, subject TEXT NOT NULL, monthlySalaryPaise INTEGER NOT NULL, joinedOn TEXT NOT NULL, isActive INTEGER NOT NULL)")
                database.execSQL("CREATE TABLE IF NOT EXISTS teacher_payments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, teacherId INTEGER NOT NULL, billingMonth TEXT NOT NULL, paidOn TEXT NOT NULL, amountPaise INTEGER NOT NULL, method TEXT NOT NULL, FOREIGN KEY(teacherId) REFERENCES teachers(id) ON DELETE RESTRICT)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_teacher_payments_teacherId ON teacher_payments(teacherId)")
            }
        }
        fun create(context: Context) = Room.databaseBuilder(context.applicationContext, TutionDatabase::class.java, "tution.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
    }
}
