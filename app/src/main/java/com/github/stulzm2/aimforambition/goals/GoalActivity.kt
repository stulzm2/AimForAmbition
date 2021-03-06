package com.github.stulzm2.aimforambition.goals

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import com.github.stulzm2.aimforambition.MainActivity
import com.github.stulzm2.aimforambition.R
import com.github.stulzm2.aimforambition.database.DatabaseHandler
import com.github.stulzm2.aimforambition.models.Goal
import kotlinx.android.synthetic.main.activity_goal.*
import java.text.SimpleDateFormat
import java.util.*
import android.view.inputmethod.InputMethodManager
import android.view.WindowManager
import android.text.Editable
import android.text.TextWatcher

/**
 * Created by matthewstulz on 2/4/18.
 */
class GoalActivity : AppCompatActivity() {

    private var dbHandler: DatabaseHandler? = null
    private var isEditMode = false
    private var cal = Calendar.getInstance()
    private var priority = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initDB()
        initOperations()
    }

    private fun isChecked() {
        simpleswitch_deadline.setOnCheckedChangeListener { _ , isChecked ->
            if (isChecked) {
                button_dialog.visibility = View.VISIBLE
                hideSoftKeyboard()
            } else {
                button_dialog.visibility = View.GONE
            }
        }
        simpleswitch_priority.setOnCheckedChangeListener { _ , isChecked ->
            if (isChecked) {
                button_priority_goal.visibility = View.VISIBLE
                hideSoftKeyboard()
            } else {
                button_priority_goal.visibility = View.GONE
            }
        }
    }

    private fun hideSoftKeyboard() {
        if (currentFocus != null) {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
    }

    private fun initDB() {
        dbHandler = DatabaseHandler(this)
        button_delete_goal.visibility = View.GONE
        supportActionBar?.title = "New Goal"
        textview_date!!.text = ""
        textview_priority.text = ""
        isChecked()
        if (intent != null && intent.getStringExtra("Mode") == "E") {
            isEditMode = true
            supportActionBar?.title = "Edit Goal"
            // Hides keyboard on initial view inflate when editing a goal
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            val goal: Goal = dbHandler!!.getGoal(intent.getIntExtra("Id",0))
            textinput_goal.setText(goal.title)
            textinput_description.setText(goal.description)
            textview_date.text = goal.date
            textview_priority.text = goal.priority
            simpleswitch_deadline.isChecked = goal.date != ""
            simpleswitch_priority.isChecked = goal.priority != ""
            button_add_goal.text = getString(R.string.save_goal)
            button_dialog.text = getString(R.string.change_date)
            button_delete_goal.visibility = View.VISIBLE
            if (goal.priority == "Medium") {
                priority = 1
            } else if (goal.priority == "High") {
                priority = 2
            }
        }
    }

    private fun checkTitle(string: Editable) {
        if (string.toString().trim { it <= ' ' }.isNotEmpty()) {
            textinputlayout_goal.isErrorEnabled = false
        } else {
            textinputlayout_goal.error = "Goal cannot be empty"
        }
    }

    private fun initOperations() {
        button_add_goal.setOnClickListener({
            val success: Boolean
            if (!isEditMode) {
                val goal = Goal()
                goal.title = textinput_goal.text.toString()
                goal.description = textinput_description.text.toString()
                goal.date = textview_date.text.toString()
                if (textview_priority.text.toString().isEmpty()) {
                    goal.priority = "High"
                } else {
                    goal.priority = textview_priority.text.toString()
                }

                success = dbHandler?.addGoal(goal) as Boolean
            } else {
                val goal = Goal()
                goal.id = intent.getIntExtra("Id", 0)
                goal.title = textinput_goal.text.toString()
                goal.description = textinput_description.text.toString()
                goal.date = textview_date.text.toString()
                goal.priority = textview_priority.text.toString()


                success = dbHandler?.updateGoal(goal) as Boolean
            }
            if (success)
                finish()
            else {
                textinputlayout_goal.error = "Goal cannot be empty"
            }

            textinput_goal.addTextChangedListener(object : TextWatcher {
                override fun onTextChanged(string: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun beforeTextChanged(string: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(string: Editable) {
                    checkTitle(string)
                }
            })
        })

        button_delete_goal.setOnClickListener({
            val dialog = AlertDialog.Builder(this)
                    .setTitle("DANGER ZONE!")
                    .setMessage("Click 'YES' to delete the goal.")
                    .setPositiveButton("YES", { dialog, _ ->
                        val success = dbHandler?.deleteGoal(intent.getIntExtra("Id", 0)) as Boolean
                        if (success)
                            onPositiveButtonClicked()
                        //finish()
                        dialog.dismiss()
                    })
                    .setNegativeButton("NO", { dialog, _ ->
                        dialog.dismiss()
                    })
            dialog.show()
        })

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        button_dialog!!.setOnClickListener {
            DatePickerDialog(this@GoalActivity,
                    dateSetListener,
                    // selects today's date when it loads up
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        button_priority_goal.setOnClickListener {
            val singleChoiceItems = resources.getStringArray(R.array.dialog_single_choice_priority_array)
            val itemSelected = priority
            AlertDialog.Builder(this)
                    .setTitle("Priority")
                    .setSingleChoiceItems(singleChoiceItems, itemSelected) { _ , selectedIndex ->
                        when (selectedIndex) {
                            0 -> { textview_priority.text = singleChoiceItems[0] }
                            1 -> { textview_priority.text = singleChoiceItems[1] }
                            2 -> { textview_priority.text = singleChoiceItems[2] }
                        }
                    }
                    .setPositiveButton("Ok", null)
                    .setNegativeButton("Cancel", null)
                    .show()
        }
    }

    private fun onPositiveButtonClicked() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("deletion", "success")
        startActivity(intent)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateDateInView() {
        val myFormat = "MM/dd/yyyy" // format of time
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        textview_date!!.text = sdf.format(cal.time)
    }
}
