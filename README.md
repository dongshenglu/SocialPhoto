SocialPhoto
===========

**Introduction**

The application was implemented for searching images stored on www.Flickr.com based on user's the current location, and simply sharing them to user's socialnetwork via "ACTION_SEND" intent.

It includes the following subfeatures,

- A searchbox and list view for performing search and displaying search result

- A setting button for user insert a search query that is matched against photo titles, descriptions or tags and clear search history

- Search request endpoint include latitude and longitude as parameters based on user's current location

- Each search result includes the title and a thumbnail in the list view

- Tapping on a search result shows photo details such as title, tags, description geolocation, and the photo itself.

Note: If users didn't set title, tags or description in search setting dialog, or the return search result didn't include those information,
then they are not shown in photo details view

- Save recent searches in an actionbar dropdown list, select the saved search keyword, result will be shown
- Social sharing is supported in Photo detail view 


Support devices and SDK version
- The application run on Android > 4, support SDK API level from 11 to 22
- The application works on portrait and landscape mode.

**Implementation brief**

Application used Executors API to manage thread pool to download images and other relevant data via Flickr REST API and stores the structured data in local SQLite. SQLiteOpenHelper is used to manage database. For thread safty reason, a ContentProvider derived class was implemented, so that application manipulates database with a single instance of SQLite database. 

With CursorLoader and SimpleCursorAdapter, data is loaded and shown in list view.

