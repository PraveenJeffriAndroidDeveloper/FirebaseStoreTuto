package com.example.firebasesore

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.lang.StringBuilder

class MainActivity : AppCompatActivity() {
    private val personCollectionRef = Firebase.firestore.collection("persons")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnUploadData).setOnClickListener {
            val person = getOldPerson()
            savePerson(person)
            //savePerson(Person(findViewById<EditText>(R.id.etFirstName).text.toString() , findViewById<EditText>(R.id.etLastName).text.toString() , findViewById<EditText>(R.id.etAge).text.toString().toInt()))
        }

        subscribeToRealTimeDatabase()

        findViewById<Button>(R.id.btnRetrieveData).setOnClickListener {
            retrivePerson()
        }

        findViewById<Button>(R.id.btnUpdatePerson).setOnClickListener {
            val person = getOldPerson()
            val newPerson = getNewPerson()
            updatePersson(person , newPerson)
        }
    }

    private fun getNewPerson() : Map<String , Any>
    {
        val firstName = findViewById<EditText>(R.id.etNewFirstName).text.toString()
        val secondName = findViewById<EditText>(R.id.etNewLastName).text.toString()
        val age = findViewById<EditText>(R.id.etNewAge).text.toString()
        var map = mutableMapOf<String , Any>()
        if (firstName.isNotEmpty())
        {
            map["firstName"] = firstName
        }
        if (secondName.isNotEmpty())
        {
            map["secondName"] = secondName
        }
        if (age.isNotEmpty())
        {
            map["age"] = age.toInt()
        }
        return map
    }

    private fun updatePersson(person : Person , map: Map<String , Any>) = CoroutineScope(Dispatchers.IO).launch {
        val personQuery = personCollectionRef.
        whereEqualTo("firstName" , person.firstName)
            .whereEqualTo("secondName" , person.secondName)
            .whereEqualTo("age"  , person.age)
            .get()
            .await()

        if (personQuery.documents.isNotEmpty())
        {
            for (document in personQuery)
            {
                try {
                    personCollectionRef.document(document.id).set(
                        map ,
                        SetOptions.merge()
                    ).await()
                }
                catch (e:Exception)
                {
                    withContext(Dispatchers.Main)
                    {
                        Toast.makeText(this@MainActivity , e.message , Toast.LENGTH_SHORT).show()
                    }
                }

            }
        }
        else
        {
            withContext(Dispatchers.Main)
            {
                Toast.makeText(this@MainActivity , "No records found" , Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun getOldPerson() : Person
    {
        return Person(findViewById<EditText>(R.id.etFirstName).text.toString() , findViewById<EditText>(R.id.etLastName).text.toString() , findViewById<EditText>(R.id.etAge).text.toString().toInt())
    }

    private fun subscribeToRealTimeDatabase()
    {
        personCollectionRef.addSnapshotListener { querySnapShot, firebaseFireStoreException ->
            firebaseFireStoreException?.let {
                Toast.makeText(this , it.message.toString() , Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            try {
                querySnapShot?.let {
                    val sb = StringBuilder()
                    for(documents in it.documents)
                    {
                        val person = documents.toObject<Person>()
                        sb.append("$person\n")
                    }
                    findViewById<TextView>(R.id.tvPersons).text = sb.toString()
                }
            }
            catch (e:Exception)
            {
                //Log.e("Error da Jeffri" , e.message.)
                Toast.makeText(this , e.message , Toast.LENGTH_SHORT).show()
            }
        }


    }

    private fun retrivePerson() = CoroutineScope(Dispatchers.IO).launch {
        try {
            var fromAge = findViewById<EditText>(R.id.etFrom).text.toString().toInt()
            var toAge = findViewById<EditText>(R.id.etTo).text.toString().toInt()
            val querySnapshot = personCollectionRef
                .whereGreaterThan("age" , fromAge)
                .whereLessThan("age" , toAge)
                .orderBy("age")
                .get()
                .await()
            val sb = StringBuilder()
            for (documents in querySnapshot.documents )
            {
                val person = documents.toObject<Person>()
                sb.append("$person\n")
            }
            withContext(Dispatchers.Main)
            {
                findViewById<TextView>(R.id.tvPersons).text = sb.toString()
            }
        }

        catch (e:Exception)
        {
            withContext(Dispatchers.Main)
            {
                Toast.makeText(this@MainActivity , e.message , Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePerson(person:Person) = CoroutineScope(Dispatchers.IO).launch {
        try {

            personCollectionRef.add(person).await()
            withContext(Dispatchers.Main)
            {
                Toast.makeText(this@MainActivity , "Successfully Uploaded" , Toast.LENGTH_LONG).show()
            }

        }
        catch (e:Exception)
        {
            withContext(Dispatchers.Main){
                Toast.makeText(this@MainActivity , e.message , Toast.LENGTH_LONG).show()
            }
        }
    }
}