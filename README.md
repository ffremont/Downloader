```rawtext
██████╗  ██████╗ ██╗    ██╗███╗   ██╗██╗      ██████╗  █████╗ ██████╗ ███████╗██████╗ 
██╔══██╗██╔═══██╗██║    ██║████╗  ██║██║     ██╔═══██╗██╔══██╗██╔══██╗██╔════╝██╔══██╗
██║  ██║██║   ██║██║ █╗ ██║██╔██╗ ██║██║     ██║   ██║███████║██║  ██║█████╗  ██████╔╝
██║  ██║██║   ██║██║███╗██║██║╚██╗██║██║     ██║   ██║██╔══██║██║  ██║██╔══╝  ██╔══██╗
██████╔╝╚██████╔╝╚███╔███╔╝██║ ╚████║███████╗╚██████╔╝██║  ██║██████╔╝███████╗██║  ██║
╚═════╝  ╚═════╝  ╚══╝╚══╝ ╚═╝  ╚═══╝╚══════╝ ╚═════╝ ╚═╝  ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝
```

Le programme downloader permet de télécharger des fichiers en automatique sans avoir besoin du navigateur.
Pour ce faire, il suffit de récupérer le lien du fichier / vidéo / image et de le placer dans un fichiers "files.txt"


![Ecran de téléchargement](http://res.cloudinary.com/dyuwlqafx/image/upload/v1478425598/Capture_d_%C3%A9cran_-_06112016_-_10_45_21_y374ra.png)
![Ecran de configuration](http://res.cloudinary.com/dyuwlqafx/image/upload/v1478425599/Capture_d_%C3%A9cran_-_06112016_-_10_45_48_dwj55a.png)

### Fonctionnalités
* téléchargement en automatique des fichiers en HTTP
* relecture des éléments à télécharger tous les X secondes (paramètrable)
* paramètrage du nombre de fichiers téléchargés à un instant T
* édition du fichier de configuration via une interface web
* annuler les téléchargements en cours via l'interface web
* recherche des téléchargements réalisés

### Utilisation
* télécharger java 8 et [installer java 8](http://www.java.com/fr/download/)
* télécharger le programme  en version 1.6 [ici](https://drive.google.com/open?id=0B3RZ6sP4kUBAeTV3S0gyLXcybkE)
* alimenter le fichier files.txt à côté du programme java (*.jar) ou faite-le via l'interface web

```text
la vidéo de mes vacances été 2015 :: http://sur-un-joli-site.com/mes-vacances15

la vidéo de mes vacances été 2016 :: http://sur-un-joli-site.com/mes-vacances16
```
* le programme va créer un répertoire **files/** qui contiendra les téléchargements (paramètrable)
* lançer le programme java [-options] -jar xxxxx.jar

### Editer le fichier de configuration
* Rendez-vous sur http://[adresse-de-mon-serveur]:[port]
    * par défaut, le port est 4567 et si vous être sur votre machine "localhost" pour l'adresse du serveur

### Command line interface

Syntaxe : java [-options] -jar xxxxx.jar

* où options :
    * -D
        * conf : chemin vers le fichier de configuration, par défaut "./files.txt"
        * files : chemin du répertoire de destination, par défaut "files/" qui sera créé automatique
        * threads : nb. de threads qui vont ếté disponible pour traiter les téléchargements à un instant T, par défaut 3
        * delay : interval de temps en secondes entre 2 relectures du fichier de configuration
        * port : port du serveur web pour éditer le fichier de configuration, par défaut 4567
        * retry : si le lien est en échec, on retente encore N fois

### Utilisations possibles
* télécharger régulièrement des fichier depuis internet (musiques, photos, vidéos)
* installer ce programme sur un serveur, Raspberry... pour avoir des fichiers disponibles...

Licence MIT
