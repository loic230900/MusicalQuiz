# MusicalQuiz Application


## Structure
```
musicalquiz/
├── README.md                             # Documentation du projet
├── app/
│   ├── manifests/
│   │   └── AndroidManifest.xml           # Déclaration des composants Android
│   ├── kotlin+java/
│   │   └── com.example.musicalquiz/
│   │       ├── MainActivity.kt           # Activité hôte unique
│   │       ├── adapter/                  # Adaptateurs RecyclerView
│   │       ├── database/                 # Base de données Room
│   │       │   ├── dao/
│   │       │   ├── entities/
│   │       │   │   └── Playlist.kt       # Entité Playlist (Quiz à ajouter)
│   │       ├── model/                    # Modèles de données Deezer
│   │       │   ├── Album.kt
│   │       │   ├── Artist.kt
│   │       │   ├── DeezerSearchResponse.kt
│   │       │   └── Track.kt
│   │       ├── network/                  # API & Retrofit
│   │       │   ├── DeezerApiInterface.kt
│   │       │   └── RetrofitInstance.kt
│   │       ├── view/
│   │       │   └── fragments/
│   │       │       ├── SearchFragment.kt
│   │       │       ├── DetailsFragment.kt
│   │       │       ├── PlaylistFragment.kt
│   │       │       └── QuizFragment.kt
│   │       └── viewmodel/
│   │           ├── TracksViewModel.kt
│   │           └── PlaylistViewModel.kt
│
├── res/
│   ├── layout/
│   │   ├── activity_main.xml             # Conteneur NavHost + BottomNavigationView
│   │   ├── fragment_search.xml           # UI de recherche
│   │   ├── fragment_details.xml          # UI détails d’un morceau etc.
│   │   ├── fragment_playlist.xml         # UI gestion des playlists
│   │   ├── fragment_quiz.xml             # UI gestion/jeu des quiz
│   ├── menu/
│   │   └── bottom_nav_menu.xml           # Menu de navigation inférieur
│   ├── navigation/
│   │   └── nav_graph.xml                 # Navigation Graph
│   ├── drawable/, mipmap/, values/, xml/
│
└── build.gradle.kts, settings.gradle.kts, etc.

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

- [x] **Create Data Classes**:
  - [x] `Track` (track id, title, duration, artist, album)
  - [x] `Album` (album id, title, cover image URL)
  - [x] `Artist` (artist id, name, picture URL)
  - [x] `DeezerSearchResponse` (list of tracks returned from API)

- [x] **Set up Retrofit Instance** (`RetrofitInstance.kt`):
  - [x] Singleton instance setup with Deezer base URL and Gson converter

- [x] **Define Deezer API Interface** (`DeezerApiInterface.kt`):
  - [x] Create Retrofit service interface with endpoint methods
  - [x] Annotate endpoints properly (`@GET`, `@Query`)

- [x] **Test a basic API call** (log results) for verification

---

### Step 3 — ViewModel Layer

- [x] **Create `TracksViewModel.kt`**:
  - [x] Store search result data (`MutableLiveData`)
  - [x] Integrate Retrofit calls via coroutines
  - [x] Handle loading state and error states clearly

- [x] **Create `PlaylistViewModel.kt`**:
  - [x] Manage playlists (`MutableLiveData`)
  - [ ] Integrate Room database interactions via coroutines ( sera fait dans les étapes suivantes)
  - [ ] CRUD operations clearly defined (create, read, update, delete)

- [x] **Verify LiveData** retains state through orientation changes (observe data in logs or UI)

---
### Step 4 — View Layer & Fragments

> Les fragments sont mis en place avec leurs layouts de base.  
> Le contenu (UI & logique métier) sera développé dans les étapes suivantes.

#### Create and Setup Fragments
- [x] `SearchFragment.kt` — UI de recherche (barre de recherche, résultats à venir dans Step 5)
- [x] `DetailsFragment.kt` — Affichage d’un album ou d’un morceau sélectionné (logique à compléter Step 7)
- [x] `PlaylistFragment.kt` — Affichage et gestion des playlists (lié à Room dans Step 6)
- [x] `QuizFragment.kt` — Interface de base du quiz (logique gameplay dans Step 7)

#### Layout XML associés
- [x] `fragment_search.xml` — Placeholder "Recherche"
- [x] `fragment_details.xml` — Placeholder "Détails"
- [x] `fragment_playlist.xml` — Placeholder "Playlists"
- [x] `fragment_quiz.xml` — Placeholder "Quiz"

#### Navigation
- [x] `BottomNavigationView` intégré dans `activity_main.xml`
- [x] `nav_graph.xml` créé dans `res/navigation`
- [x] Navigation fonctionnelle (testée et reliée via `NavController` dans `MainActivity.kt`)

---

### Step 5 — RecyclerView & Adapter

#### UI RecyclerView
- [ ] Créer `track_item.xml` avec :
  - [ ] Image de la pochette d’album (ImageView)
  - [ ] Titre du morceau (TextView)
  - [ ] Nom de l’artiste (TextView)

#### Adapter
- [ ] Implémenter `TrackAdapter.kt`
  - [ ] Hériter de `RecyclerView.Adapter`
  - [ ] Créer `TrackViewHolder`
  - [ ] Lier les données avec Glide

#### Tests
- [ ] Tester l’affichage des résultats dans `SearchFragment`
- [ ] Vérifier chargement images, clics, scroll fluide

---

### 🗃️ Step 6 — Database (Room)

#### Entités
- [ ] `Playlist.kt`
- [ ] `Quiz.kt`
- [ ] Annotations : `@Entity`, `@PrimaryKey`, relations

#### DAO
- [ ] `PlaylistDao.kt`
- [ ] `QuizDao.kt`
- [ ] Fonctions : insert, delete, update, getAll

#### Base de données
- [ ] `AppDatabase.kt`
  - [ ] Annotations `@Database`
  - [ ] Singleton via `getInstance()`

#### Tests
- [ ] Insérer et lire des playlists/quiz
- [ ] Observer les résultats via `LiveData`

---

### Step 7 — Playlist and Quiz Management

#### Gestion de playlists
- [ ] UI pour créer et nommer une nouvelle playlist
- [ ] Ajouter un morceau via clic long
- [ ] Liste des playlists avec suppression/édition

#### Gestion de quiz
- [ ] Création de quiz depuis une playlist (ou librement)
- [ ] Prévisualisation audio
- [ ] Sélection aléatoire ou personnalisée des questions

#### Gameplay
- [ ] Mode QCM (réponses multiples)
- [ ] Lecture preview Deezer
- [ ] Résultat / score utilisateur

---

### Step 8 — UI/UX Refinement

#### Design Material
- [ ] Harmoniser les couleurs, tailles, espacements
- [ ] Utiliser `MaterialTheme`, `CardView`, `ShapeableImageView`...

#### Expérience utilisateur
- [ ] Indiquer les erreurs (ex: pas de réseau)
- [ ] Utiliser `Snackbar`, `ProgressBar`, `Toast`

#### Animations / Transitions
- [ ] Transitions entre fragments
- [ ] Animation lors du chargement de contenu

---

### Step 9 — Testing & Debugging

#### Fonctionnalités principales
- [ ] API Deezer — recherche et parsing
- [ ] Sauvegarde et affichage des playlists

#### Robustesse
- [ ] Rotation écran sans perte d’état
- [ ] Comportement sur plusieurs appareils (emulateur + device)

#### Performances
- [ ] Glide : cache efficace, memory friendly
- [ ] Limiter les appels réseau redondants

---

###  Step 10 — Finalization

