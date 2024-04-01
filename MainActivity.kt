package com.example.weather_assignment


import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.weather_assignment.WeatherHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

var flag1_location = false
var flag2_date= false
var flag3 = false
var flag4 = false


var maxTemp by mutableStateOf(0.0)
var minTemp by mutableStateOf(0.0)

private const val REQUEST_WRITE_STORAGE = 1001

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp(LocalContext.current)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now save weather data
            } else {
                // Permission denied
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}



@Composable
fun DatePicker(selectedDate: Date, onDateSelected: (Date) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var buttonText by remember { mutableStateOf("Select Date") } // Use remember with mutableStateOf

    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .wrapContentHeight()
    ) {
        Text(
            text = "Selected Date: ${SimpleDateFormat.getDateInstance().format(selectedDate)}",
            modifier = Modifier.padding(8.dp)
        )

        Button(
            onClick = {
                val calendar = Calendar.getInstance()
                calendar.time = selectedDate

                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)
                val day = calendar.get(Calendar.DAY_OF_MONTH)

                val datePickerDialog = DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        calendar.set(year, month, day)
                        val newSelectedDate = calendar.time
                        onDateSelected(newSelectedDate) // Update selected date
                        buttonText = "Select Date: ${SimpleDateFormat.getDateInstance().format(newSelectedDate)}"
                    },
                    year,
                    month,
                    day
                )

                datePickerDialog.show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(buttonText)
        }
    }
}

fun isNetworkConnected(context: Context): Boolean
{
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetworkInfo = connectivityManager.activeNetworkInfo
    return activeNetworkInfo != null && activeNetworkInfo.isConnected
}

@Composable
fun MyApp(context: Context)
{
    var selectedDate by remember { mutableStateOf(Date()) }
    var location by remember { mutableStateOf("") }
    var minTemp by remember { mutableStateOf<Double?>(null) }
    var maxTemp by remember { mutableStateOf<Double?>(null) }
    val isConnected = isNetworkConnected(context)

    LaunchedEffect(Unit)
    {
        if (!isConnected)
        {
            val weatherRepo = WeatherRepository(WeatherDatabase.getInstance(context))
            val weatherData = weatherRepo.getWeatherData(location, SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate))
            minTemp = weatherData?.minTemp
            maxTemp = weatherData?.maxTemp
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(135, 206, 235))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    )

    {
        DatePicker(
            selectedDate = selectedDate,
            onDateSelected = { newDate ->
                selectedDate = newDate
            },
            modifier = Modifier
        )

        LocationInput(location = location) { text ->
            location = text
        }

        // Display "Fetch Weather" button text
        Text(
            text = "Fetch Weather",
            style = TextStyle(fontSize = 20.sp, color = Color.Blue),
            modifier = Modifier.clickable {
                CoroutineScope(Dispatchers.IO).launch {
                    val calendar = Calendar.getInstance()
                    calendar.time = selectedDate
                    val year = calendar.get(Calendar.YEAR)

                    if ((year < 1950) || (year>2034)) {
                        // Show alert dialog for invalid year
                        withContext(Dispatchers.Main) {
                            showAlertDialog(context, "Invalid year")
                        }
                    }
                    else
                    {
                        fetchWeatherData(selectedDate, location, context) { min, max ->
                            minTemp = min
                            maxTemp = max
                        }
                    }
                }

            }


        )

        minTemp?.let { min ->
            Text(
                text = "Min Temperature: ${min}°C",
                modifier = Modifier.padding(8.dp)
            )
        }

        maxTemp?.let { max ->
            Text(
                text = "Max Temperature: ${max}°C",
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

fun showAlertDialog(context: Context, message: String) {
    val builder = AlertDialog.Builder(context)
    builder.setTitle("Error")
        .setMessage(message)
        .setPositiveButton("OK") { dialog, _ ->
            // Dismiss the dialog when OK button is clicked
            dialog.dismiss()
        }
    val dialog = builder.create()
    dialog.show()
}
suspend fun fetchWeatherData(
    selectedDate: Date,
    location: String,
    context: Context,
    onWeatherFetched: (Double?, Double?) -> Unit // Callback function to handle weather data
) {
    // Check if the selected date is valid (between 1950 and 2034)
    val currentDate = Calendar.getInstance().time

    // Launch a coroutine to fetch weather data
    val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate)
    val date = formattedDate.toString()

    if (isNetworkConnected(context)) {
        if (selectedDate > currentDate) {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://weather.visualcrossing.com/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()

            val weatherApiService = retrofit.create(WeatherApiS::class.java) // Future date
            GlobalScope.launch(Dispatchers.IO) {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val weatherDataMax = mutableListOf<Double>()
                val weatherDataMin = mutableListOf<Double>()
                var responseData: String
                val formattedDate =
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate)
                val date = formattedDate.toString()
                for (year in (currentYear - 10) until currentYear) {
                    responseData = weatherApiService.getWeather(
                        location,
                        "$year-${date.substring(5)}"
                    )
                    println("Weather API Response for $year: $responseData")
                    // parsing the response and extracting max and min temperatures
                    val jsonObject = JSONObject(responseData)
                    val daysArray = jsonObject.getJSONArray("days")
                    val firstDay = daysArray.getJSONObject(0)
                    val maxTempF = firstDay.getDouble("tempmax")
                    val minTempF = firstDay.getDouble("tempmin")
                    weatherDataMax.add(maxTempF)
                    weatherDataMin.add(minTempF)
                }
                val averageMaxTempF =
                    String.format("%.2f", weatherDataMax.average()).toDouble()
                val averageMinTempF =
                    String.format("%.2f", weatherDataMin.average()).toDouble()
                val maxTemp = fahrenheitToCelsius(averageMaxTempF)
                val minTemp = fahrenheitToCelsius(averageMinTempF)

                responseData = weatherApiService.getWeather(
                    location,
                    formattedDate.toString()
                )
                val jsonObj = JSONObject(responseData)

                // retrieve the days array from the JSON object
                val daysArray = jsonObj.getJSONArray("days")

                if (daysArray.length() > 0) {
                    val firstDay = daysArray.getJSONObject(0)
                    firstDay.put("tempmax", averageMaxTempF)
                    firstDay.put("tempmin", averageMinTempF)

                    // Replace the existing "tempmax" and "tempmin" values with the updated ones
                    firstDay.remove("tempmax")
                    firstDay.remove("tempmin")
                    firstDay.put("tempmax", averageMaxTempF)
                    firstDay.put("tempmin", averageMinTempF)
                }

                // converting the JSONObject back to a JSON string with indentation
                val updatedResponseData = jsonObj.toString()
                val weatherEntity = WeatherHistory(
                    location = location,
                    date = formattedDate,
                    maxTemp = maxTemp,
                    minTemp = minTemp,
                    weatherDetails = updatedResponseData.toString()
                )

                saveToStorage(context, responseData)
                insertWeatherDataIntoDatabase(weatherEntity, context)
                onWeatherFetched(minTemp, maxTemp)
            }

        } else {
            try {

                // Current or past date
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://weather.visualcrossing.com/")
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build()

                val weatherApiService = retrofit.create(WeatherApiS::class.java)
                val responseData =
                    weatherApiService.getWeather(location, formattedDate.toString())
                // Process weather data
                val jsonObject = JSONObject(responseData)
                val daysArray = jsonObject.getJSONArray("days")
                val firstDay = daysArray.getJSONObject(0)
                val maxTempF = firstDay.getDouble("tempmax")
                val minTempF = firstDay.getDouble("tempmin")
                val maxTempC = fahrenheitToCelsius(maxTempF)
                val minTempC = fahrenheitToCelsius(minTempF)

                // Pass weather data to callback function
                onWeatherFetched(minTempC, maxTempC)

                // Insert weather history into database
                val weatherEntity = WeatherHistory(
                    location = location,
                    date = formattedDate,
                    maxTemp = maxTempC,
                    minTemp = minTempC,
                    weatherDetails = firstDay.toString()
                )
                saveToStorage(context, responseData)
                insertWeatherDataIntoDatabase(weatherEntity, context)
                Log.d("WeatherApp", "Weather data inserted into the database: $weatherEntity")
            }catch (e: HttpException) {
                val errorResponse = e.response()?.errorBody()?.string()
                if (errorResponse != null && errorResponse.contains("Bad API Request:Invalid location parameter value.")) {
                    // Show alert dialog for invalid location
                    showAlertDialog(context, "Invalid location")
                }
            }
            catch(e: Exception){


                    showAlertDialog(context, "Invalid location")

            }
        }
    } else {
        try {
            // Network not connected
            if (selectedDate > currentDate) { // Future date
                // Take the average of 10-year min and max temp from repository
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val weatherDataMax = mutableListOf<Double>()
                val weatherDataMin = mutableListOf<Double>()
                val weatherRepo =
                    WeatherRepository(WeatherDatabase.getInstance(context))

                for (year in (currentYear - 10) until currentYear) {

                    val yearData = weatherRepo.getWeatherData(
                        location, "$year-${date.substring(5)}"
                    )
                    if (yearData != null) {
                        weatherDataMax.add(yearData.maxTemp)
                        weatherDataMin.add(yearData.minTemp)
                    }
                }
                val averageMaxTempC =
                    String.format("%.2f", weatherDataMax.average()).toDouble()
                val averageMinTempC =
                    String.format("%.2f", weatherDataMin.average()).toDouble()
                val maxTemp = averageMaxTempC
                val minTemp = averageMinTempC
                onWeatherFetched(minTemp, maxTemp)
            } else {
                // Current or past date
                // Fetch weather data from repository if not connected
                val weatherRepo =
                    WeatherRepository(WeatherDatabase.getInstance(context))
                val weatherData =
                    weatherRepo.getWeatherData(location, formattedDate)
                if (weatherData != null) {
                    onWeatherFetched(weatherData.minTemp, weatherData.maxTemp)
                }
            }
        } catch (e: HttpException) {
            // Handle HTTP errors
            val errorResponse = e.response()?.errorBody()?.string()
            if (errorResponse!=null&& errorResponse.contains("Bad API Request:Invalid location parameter value.")) {
                // Show alert dialog for invalid location
                showAlertDialog(context, "Invalid location")
            } else {
                // Show generic error message
//                showToast("Failed to fetch weather data", context)
            }
            Log.e("WeatherApp", "Weather API Error Response: $errorResponse")
        } catch (e: Exception) {
            // Handle other exceptions
            e.printStackTrace()
            //        showToast("Failed to fetch weather data", context)
        }

    }
}

fun saveToStorage(context: Context, weatherData: String) {

    val fileName = "my_weather_details.json"

    // Checking for permission
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        // Request permission
        if (context is Activity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_STORAGE
            )
        } else {
            // Handle the case when the context is not an activity
            Log.e("SaveWeatherData", "Context is not an Activity")
        }
        return
    }

    val directory = getStorageDirectory(context)
    if (directory != null) {
        val file = File(directory, fileName)
        try {
            FileOutputStream(file).use { outputStream ->
                outputStream.write(weatherData.toByteArray())
            }
            Log.d("SaveWeatherData", "Weather data saved successfully to: ${file.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("SaveWeatherData", "Failed to save weather data: ${e.message}")
        }
    } else {
        Log.e("SaveWeatherData", "Failed to get storage directory")
    }
}


// Function to get the appropriate storage directory
private fun getStorageDirectory(context: Context): File? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // For Android Q (10) and above
        println("hi")
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    } else {
        // For older versions
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    }
}




private suspend fun insertWeatherDataIntoDatabase(weatherEntity: WeatherHistory, context: Context) {
    withContext(Dispatchers.IO) {
        // Log the weather entity before inserting into the database
        Log.d("WeatherApp", "Weather entity to be inserted: $weatherEntity")

        val weatherRepo = WeatherRepository(WeatherDatabase.getInstance(context))
        weatherRepo.insertWeatherData(weatherEntity)
    }
}



private fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

// Request permission to write to external storage
private fun requestStoragePermission(activity: ComponentActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        activity.requestPermissions(
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            REQUEST_WRITE_STORAGE
        )
    }
}


//fun showToast(message: String, context: android.content.Context) {
//    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
//}


fun fahrenheitToCelsius(fahrenheit: Double): Double {
    return ((fahrenheit - 32) * 5 / 9).toDouble().let {
        // Round to one decimal place
        String.format("%.1f", it).toDouble()
    }
}

@Composable
fun LocationInput(location: String, onLocationChange: (String) -> Unit) {
    var text by remember { mutableStateOf(location) }
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onLocationChange(it)
        },
        label = { Text("Enter Location") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
        modifier = Modifier.fillMaxWidth()
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApp(LocalContext.current)
}