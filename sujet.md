# Project *MusicalQuiz* - Music Exploration and Quiz

## Objective

Develop an app that uses the Deezer API to allow users to:

- Search for music tracks.
- View and interact with search results.
- Create and manage custom playlists.
- Listen to short track previews.
- Create and play quiz where users guess the song from short previews.

## Submission guidelines

- This project must be completed in pairs
- You can develop your project in the `Project` folder of your `mobile-application-development` git repository
- Don't forget to commit and push your final project
- Fill this shared document and indicate your pair composition and the link to your repository:
  https://docs.google.com/spreadsheets/d/1tK3rzjlDuCQ7-Th3eW9tAywaFMwLrN9vXAwL_oqhf0g/edit?usp=sharing
- Project deadline: **23th May**. Any commits made after this deadline will not be considered
- Projects demonstrations will be held the 26th May. A schedule with your presentation time will be provided.

## Report

A report must be provided in a `README.md` file and should include the three following sections:

- A first section summarizing the functionalities of your app and including some illustrative screenshots.

- A second, more technical section describing your app architecture using a diagram and explaining the implementation choices made.

- A third section outlining the technical problems that are remain unsolved.

Additionally, the report must clearly indicate the contribution of each pair to the project.

## Specifications

The app is composed of a single activity and multiple fragments. Each screen of the app is hosted by a dedicated fragment.
The navigation between the screens is based on a bottom navigation bar.

The app is composed of the following elements:

### Search screen

The search screen enables the user to search for music tracks.
It is composed of:

- a search bar
- an option to search for tracks or albums
- a button to launch the search

The results are displayed in a `RecyclerView` under the search button. The items should be displayed as a scrollable grid.

Items should display the album cover as an image, the album title and/or the track title and the artist name.

When an item is clicked, the details of the item are displayed in the "Details screen"

When an item is long clicked, the item is added to the current playlist.


### Details screen

This screen shows detailed informations regarding a specific track or album.
If it is a track, it should include a button to listen for a short preview of the track.

### Playlist screen

This screen allows the user to list, create and manage the local playlists.
Each playlist is composed of different tracks.


### Quiz screen

The screen allows the user to list, create, manage the local quiz.
A quiz is associated to a playlist.

When the user clicks on a quiz, it displays a dedicated fragment with all the informations on a specific quiz:

- Name of the quiz
- Name of the playlist associated to the quiz
- A button to launch the quiz

The questions (tracks) can either be selected randomly or pre-determined in advance.

When the quiz starts:

- For each question, a preview of the track is played
- Depending on the game mode:
    - The user will either choose from multiple-choice answers (one correct answer and other incorrect ones)
    - Or answer open-ended questions.

### Local database

The playlists and quiz are stored in a local app database (you can use Room for example).

## Implementation choices

Some screens are not described in detail: it’s up to you to propose implementation options.

## Mandatory features

Your project must:

- be robust to configuration changes
- contain at least one RecyclerView
- contain at least one ViewModel
- contain a local SQLite database
- be based on a MVVM architecture


## Optional Advanced Features

### Several game modes
- **Multiple choices:** Listen to a track preview and guess the answer from multiple-choices
- **Fill in the Blanks:** Provide the correct response with missing letters in the answer.
- **Configurable Time Limit:** Set a custom time limit for each question.

### Offline mode

- Cache recent searches or playlists for offline access.  

