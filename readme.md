# Objectif du projet

Ce projet contient le code permettant de contrôler un environnement IMX :
 * arrêt / démarrage serveur IMX
 * récupération des patchs du serveur de fabrication
 * installation de patchs.

 
# Exemple d'utilisation

Se reporter aux classes de test unitaire


# Outils necessaires

Telecharger Groovy, Gradle et le Jdk 1.8 (pas sûr pour ce dernier)
Telecharger Jenkins aussi.
En option : Elipse Mars avec le plugin Groovy et le plugin Eclipse BuildShip.


# Construction environnement de développement

Une fois les outils installés, il faudra - sur un PC pouvant se connecter sur internet sans proxy bloquant les URLs - 
lancer la commande suivante à la racine du projet :

    # attention modifier si besoin les variables JAVA_HOME et GRADLE_HOME du fichier gradle-env.bat
    gradle-env.bat
    gradle -g c:\data\.gradle compile

Cela permettra à gradle de récupérer les dépendances et de les installer sur c:\data\.gradle.
Il suffira ensuite de recopier ces dépendances sur les autres PC (en conservant le nom complet du répertoire) 
pour ne plus avoir à réaliser cette étape.


# Publication du driver Oracle

    #cela a pour effet de rajouter le driver Oracle dans le repo maven (C:\data\.m2)
    cd C:\dev\eclipse-jee-mars-1-win32\workspace\imx-gradle-plugin
    gradle-env.bat
    cd ojdbc6
    gradle -g c:\data\.gradle publishToMavenLocal
    gradle -g c:\data\.gradle publish


# Développement & Maintenance du projet

Les commandes suivantes sont à exécuter sur le répetoire de base du projet :
 * clean : gradle -g c:\data\.gradle clean
 * compile : gradle -g c:\data\.gradle compileGroovy
 * test : gradle -g c:\data\.gradle test
 * publier vers maven local : gradle -g c:\data\.gradle publishToMavenLocal

Les tests unitaires se basent sur :
 * le repository com/up/imx/repository-test.json.
 * sur les infos de connexion contenues dans la classe de test ImxTest.
 * sur la clé de connexion SSH .ssh/id_rsa

A chaque fois que vous modifiez une classe, assurez vous que les tests unitaires tournent correctement :

    gradle -g c:\data\.gradle test

Note : pour exécuter une seule classe de test, utiliser la propriété système test.single
    
    gradle -g c:\data\.gradle -Dtest.single=ImxRepository test 

Une fois le développement terminé, publier le projet sur le maven local :
    
    gradle -g c:\data\.gradle clean publishToMavenLocal

Pour publier sur jenkins :
    
    gradle -g c:\data\.gradle clean publish

Il sera disponible sous C:\Users\<utilisateur>\.m2.
Il faudra avoir le répertoire .m2 sur toutes les machines utilisant le projet (ie. Jenkins)


# Références

Manuel utilisateur Groovy, Grails

 
 
