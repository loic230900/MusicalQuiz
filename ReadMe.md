# MusicalQuiz Application


## Structure
```
musicalquiz/
├── README.md                         # Project documentation and report
├── app/
   ├── manifests/                    # Android manifest file
   ├── kotlin+java/
   │   └── com.example.musicalquiz/
   │       ├── MainActivity.kt       # Single host activity for fragments
   │       ├── adapter/              # RecyclerView adapters
   │       │   └── TrackAdapter.kt
   │       ├── database/             # Local database (Room)
   │       │   ├── dao/
   │       │   │   ├── PlaylistDao.kt
   │       │   │   └── QuizDao.kt
   │       │   ├── entities/
   │       │   │   ├── Playlist.kt
   │       │   │   └── Quiz.kt
   │       │   └── AppDatabase.kt
   │       ├── model/                # Data models
   │       │   ├── DeezerSearchResponse.kt
   │       │   ├── Track.kt
   │       │   ├── Album.kt
   │       │   └── Artist.kt
   │       ├── network/              # Retrofit setup
   │       │   ├── RetrofitInstance.kt
   │       │   └── DeezerApiInterface.kt
   │       ├── view/                 # UI components (Fragments)
   │       │   └── fragments/
   │       │       ├── SearchFragment.kt
   │       │       ├── DetailsFragment.kt
   │       │       ├── PlaylistFragment.kt
   │       │       └── QuizFragment.kt
   │       └── viewmodel/            # ViewModels
   │           ├── TracksViewModel.kt
   │           └── PlaylistViewModel.kt
   └── res/                          # Resources (layouts, drawables, strings)
       ├── drawable/
       ├── layout/
       ├── values/
       └── mipmap/

```
## Application Architecture

**Architecture**: MVVM (Model-View-ViewModel)

- **Model**:
    - Retrofit & Deezer API integration
    - Room Database for persistence
- **View**:
    - Single Activity (`MainActivity`) with multiple fragments.
    - Navigation using BottomNavigationView.
- **ViewModel**:
    - Data persistence across configuration changes.
    - Interaction logic between View and Model.

## Main Components and Technologies

### Retrofit (API Integration)

- Deezer API for music search and track previews.
- Asynchronous requests with coroutines.

### RecyclerView

- Display search results and playlists.
- Grid-based layout for aesthetic UI.

### Glide

- Efficiently loading album cover images from URLs.

### Room Database

- Persist user-generated playlists and quizzes locally.
- Entities: `Playlist`, `Quiz`.

### MVVM Architecture

- Clear separation of concerns between Model, View, and ViewModel.
- Robustness against configuration changes.

---

# Step-by-Step Implementation Checklist

---

### Step 1 — Project Initialization

- [x] **Open Android Studio and Create a New Project**:
  - [x] Select **Empty Views Activity**
  - [x] Set language to **Kotlin**
  - [x] Select **API Level 29 (Android 10)**

- [x] **Add Dependencies** (in `app/build.gradle.kts`):
  - [x] **Retrofit** (HTTP requests)
  - [x] **Room** (local database)
  - [x] **Glide** (image loading)
  - [x] **ViewModel & LiveData** (architecture components)
  - [x] **RecyclerView** (scrollable lists)
  - [x] **Navigation Components** (fragment management)

- [x] **Sync Gradle and Verify** project builds without errors

- [x] **Run the empty project** to verify initial setup

---

### Step 2 — Model Layer (Data Classes & Retrofit)

- [ ] **Create Data Classes**:
  - [ ] `Track` (track id, title, duration, artist, album)
  - [ ] `Album` (album id, title, cover image URL)
  - [ ] `Artist` (artist id, name, picture URL)
  - [ ] `DeezerSearchResponse` (list of tracks returned from API)

- [ ] **Set up Retrofit Instance** (`RetrofitInstance.kt`):
  - [ ] Singleton instance setup with Deezer base URL and Gson converter

- [ ] **Define Deezer API Interface** (`DeezerApiInterface.kt`):
  - [ ] Create Retrofit service interface with endpoint methods
  - [ ] Annotate endpoints properly (`@GET`, `@Query`)

- [ ] **Test a basic API call** (log results) for verification

---

### Step 3 — ViewModel Layer

- [ ] **Create `TracksViewModel.kt`**:
  - [ ] Store search result data (`MutableLiveData`)
  - [ ] Integrate Retrofit calls via coroutines
  - [ ] Handle loading state and error states clearly

- [ ] **Create `PlaylistViewModel.kt`**:
  - [ ] Manage playlists (`MutableLiveData`)
  - [ ] Integrate Room database interactions via coroutines
  - [ ] CRUD operations clearly defined (create, read, update, delete)

- [ ] **Verify LiveData** retains state through orientation changes (observe data in logs or UI)

---

### Step 4 — View Layer & Fragments

- [ ] **Create Four Fragments**:
  - [ ] `SearchFragment.kt` (search UI with input and results)
  - [ ] `DetailsFragment.kt` (show detailed track or album info)
  - [ ] `PlaylistFragment.kt` (manage playlist UI and interactions)
  - [ ] `QuizFragment.kt` (create, manage, and play quiz)

- [ ] **Implement Fragment Layouts** (XML layouts clearly structured)

- [ ] **Setup Navigation Component**:
  - [ ] BottomNavigationView to switch between fragments
  - [ ] Navigation Graph defined (`nav_graph.xml`)
  - [ ] Verify correct fragment transitions and back navigation handling

---

### Step 5 — RecyclerView & Adapter

- [ ] **Design RecyclerView Item Layout** (`track_item.xml`):
  - [ ] Include album cover image, track title, artist name clearly displayed

- [ ] **Create `TrackAdapter.kt`**:
  - [ ] Extend `RecyclerView.Adapter`
  - [ ] Implement ViewHolder pattern clearly
  - [ ] Bind data from API correctly to views

- [ ] **Integrate Glide to load images** efficiently into RecyclerView items

- [ ] **Test RecyclerView** displays search results properly

---

### Step 6 — Database (Room)

- [ ] **Define Entities** (`Playlist.kt`, `Quiz.kt`):
  - [ ] Annotate clearly (`@Entity`) and define primary keys (`@PrimaryKey`)

- [ ] **Create DAOs**:
  - [ ] `PlaylistDao.kt` (insert, update, delete, get playlists)
  - [ ] `QuizDao.kt` (insert, update, delete, get quizzes)

- [ ] **Setup `AppDatabase.kt`**:
  - [ ] Annotate database class (`@Database`)
  - [ ] Implement singleton pattern to instantiate the database

- [ ] **Test Room DB interactions** (CRUD operations)

---

### Step 7 — Playlist and Quiz Management

- [ ] **Playlist Management**:
  - [ ] UI to create new playlists
  - [ ] Add/remove tracks from playlists via long-click or context menu
  - [ ] Display playlists clearly in RecyclerView

- [ ] **Quiz Management**:
  - [ ] UI for creating quizzes associated with playlists
  - [ ] Logic for random or predefined track selection for quizzes
  - [ ] Implement music preview playback via Deezer API

- [ ] **Quiz Gameplay Logic**:
  - [ ] Implement multiple-choice question handling
  - [ ] Playback audio preview clearly
  - [ ] Track user answers and provide feedback

---

### Step 8 — UI/UX Refinement

- [ ] **Apply Material Design standards** (color palette, typography, spacing, icons clearly consistent)

- [ ] **Implement Error Handling**:
  - [ ] Display clear user messages on network/database errors
  - [ ] Add progress indicators during loading states

- [ ] **Enhance User Interactions** (animations, intuitive navigation clearly implemented)

---

### Step 9 — Testing & Debugging

- [ ] **Functional Tests**:
  - [ ] Verify Deezer API interactions (successful fetch/display clearly confirmed)
  - [ ] Verify database CRUD operations clearly working as expected

- [ ] **Robustness Checks**:
  - [ ] Test screen rotations and configuration changes (no loss of data or crashes)
  - [ ] Test on emulator and physical devices

- [ ] **Performance Optimization**:
  - [ ] Check memory and network usage (Glide image caching effectively used)

---

### Step 10 — Finalization

- [ ] **Code Documentation and Clean-Up**:
  - [ ] Clearly comment methods, classes, and complex logic
  - [ ] Remove unused imports and redundant code

- [ ] **Complete and polish README.md**:
  - [ ] Include detailed explanations clearly describing functionalities
  - [ ] Add screenshots illustrating key screens (Search, Details, Playlist, Quiz)

- [ ] **Prepare Demo Presentation** clearly outlining project highlights

---