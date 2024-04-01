# MC_assignment2
The Weather Assignment App is an Android application built with Jetpack Compose, a modern UI toolkit for native Android development. The app's main objective is to provide users with up-to-date weather information for a specified location and date. It features a user-friendly interface comprising various components that facilitate the interaction between the user and the app's functionality.

One of the core components of the app is the Date Picker, which allows users to select a specific date for which they want to retrieve weather data. The Date Picker is integrated seamlessly into the app's interface, providing a smooth and intuitive user experience. Upon selecting a date, the app dynamically updates the displayed weather information to reflect the selected date's data.

Another essential element of the app is the Location Input field, where users can enter the location for which they want to fetch weather data. The Location Input field employs Jetpack Compose's capabilities to create a responsive and visually appealing text input interface. Users can enter the location either manually or by using suggestions provided by the app, enhancing usability and convenience.

Once users have selected a date and entered a location, they can initiate the weather data retrieval process by clicking the "Fetch Weather" button. This button triggers an asynchronous operation using Coroutines, a concurrency framework for Kotlin. The app communicates with an external API using Retrofit, a powerful HTTP client library for Android, to fetch weather data based on the user's input.

After fetching the weather data, the app displays relevant information such as the minimum and maximum temperatures for the selected location and date. Additionally, the app incorporates error handling mechanisms to notify users in case of invalid input or network errors. If an error occurs, the app displays an alert dialog to inform the user and prompt them to take appropriate action.

Furthermore, the app utilizes Room Database to store fetched weather data locally, enabling offline access and data persistence. This ensures that users can still access weather information even when they are not connected to the internet. By storing data locally, the app enhances performance and reduces reliance on external resources.

functionalities:-
MainActivity: Entry point of the application. Sets up the content using Jetpack Compose by calling the setContent function.
MyApp: Composable function that represents the UI of the application. It consists of a DatePicker, LocationInput, and a "Fetch Weather" button. It also displays the minimum and maximum temperatures fetched from the weather API.
DatePicker: Composable function that allows the user to select a date using a DatePickerDialog. It updates the selected date when the user makes a selection.
LocationInput: Composable function that provides a text input field for the user to enter the location for weather data retrieval.
fetchWeatherData: Function responsible for fetching weather data from an external API. It handles both current and future dates, processes the data, and saves it to local storage. It also handles network connectivity checks and error handling.
saveToStorage: Function to save weather data to external storage. It checks for permission and handles writing data to a file.
insertWeatherDataIntoDatabase: Function to insert weather data into a local database using Room. It runs on a background thread to avoid blocking the main UI thread.
fahrenheitToCelsius: Function to convert temperature from Fahrenheit to Celsius.
