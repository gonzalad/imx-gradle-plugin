# Objectif du projet

Ce projet contient le code permettant de contr�ler un environnement IMX :
 * arr�t / d�marrage serveur IMX
 * r�cup�ration des patchs du serveur de fabrication
 * installation de patchs.

 
# Exemple d'utilisation

Se reporter aux classes de test unitaire


# Outils necessaires

Telecharger Groovy, Gradle et le Jdk 1.8 (pas s�r pour ce dernier)
Telecharger Jenkins aussi.
En option : Elipse Mars avec le plugin Groovy et le plugin Eclipse BuildShip.


# Construction environnement de d�veloppement

Une fois les outils install�s, il faudra - sur un PC pouvant se connecter sur internet sans proxy bloquant les URLs - 
lancer la commande suivante � la racine du projet :

    # attention modifier si besoin les variables JAVA_HOME et GRADLE_HOME du fichier gradle-env.bat
    gradle-env.bat
    gradle -g c:\data\.gradle compile

Cela permettra � gradle de r�cup�rer les d�pendances et de les installer sur c:\data\.gradle.
Il suffira ensuite de recopier ces d�pendances sur les autres PC (en conservant le nom complet du r�pertoire) 
pour ne plus avoir � r�aliser cette �tape.


# Publication du driver Oracle

    #cela a pour effet de rajouter le driver Oracle dans le repo maven (C:\data\.m2)
    cd C:\dev\eclipse-jee-mars-1-win32\workspace\imx-gradle-plugin
    gradle-env.bat
    cd ojdbc6
    gradle -g c:\data\.gradle publishToMavenLocal
    gradle -g c:\data\.gradle publish


# D�veloppement & Maintenance du projet

Les commandes suivantes sont � ex�cuter sur le r�petoire de base du projet :
 * clean : gradle -g c:\data\.gradle clean
 * compile : gradle -g c:\data\.gradle compileGroovy
 * test : gradle -g c:\data\.gradle test
 * publier vers maven local : gradle -g c:\data\.gradle publishToMavenLocal

Les tests unitaires se basent sur :
 * le repository com/up/imx/repository-test.json.
 * sur les infos de connexion contenues dans la classe de test ImxTest.
 * sur la cl� de connexion SSH .ssh/id_rsa

A chaque fois que vous modifiez une classe, assurez vous que les tests unitaires tournent correctement :

    gradle -g c:\data\.gradle test

Note : pour ex�cuter une seule classe de test, utiliser la propri�t� syst�me test.single
    
    gradle -g c:\data\.gradle -Dtest.single=ImxRepository test 

Une fois le d�veloppement termin�, publier le projet sur le maven local :
    
    gradle -g c:\data\.gradle clean publishToMavenLocal

Pour publier sur jenkins :
    
    gradle -g c:\data\.gradle clean publish

Il sera disponible sous C:\Users\<utilisateur>\.m2.
Il faudra avoir le r�pertoire .m2 sur toutes les machines utilisant le projet (ie. Jenkins)


# R�f�rences

Manuel utilisateur Groovy, Grails

 
 
