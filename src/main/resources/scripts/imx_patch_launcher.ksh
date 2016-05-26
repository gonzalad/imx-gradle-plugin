#!/bin/ksh

#remplace le lanceur imx_patch.ksh, utilise par tache imx-gradle-plugin

PATCHES_LIST=$imxHome/patchs/patches_list
CONF=$imxHome/patchs/imx_patch.conf
AT_OUT=$imxHome/patchs/at_out

function _GET_ {
        grep "$1" $CONF | cut -d= -f2
}

# Check for any non executed patch session
if [ -f $CONF ] && [ $(_GET_ AT_JOB) ]
then
	echo "Une installation semble etre deja programmee (fichier $CONF existant):"
	cat $CONF
	echo
	echo "Liste des patchs :"
	cat $PATCHES_LIST
	printf "\n"
	echo "Annulation de la nouvelle programmation."
	exit 1
fi

# Check for patches_list
if [ ! -f $PATCHES_LIST ]
then
	echo "Fichier $PATCHES_LIST manquant"
	exit 1
fi

echo "Liste des patchs a installer (pour la modifier, editer le fichier $PATCHES_LIST) :"
cat "$PATCHES_LIST"

#Couper les services Oracle Application Server ? ")
echo "STOPINSTANCE=" > $CONF

#Couper les services Oracle Application Server ? ")
echo "STOPTEL=" >> $CONF

#En cas d'absence d'un processus, forcer l'installation des patchs (deconseille) ? ")
echo "FORCE=" >> $CONF

#Ceci est-il une reinstallation ? ")
echo "REINSTALL=" >> $CONF

LAUNCH_TIME="now + 1 minute"
echo "LAUNCH_TIME=$LAUNCH_TIME" >> $CONF
at $LAUNCH_TIME <<STOP 2>$AT_OUT
$imxHome/patchs/$doInstallScriptFilename | tee -a $imxHome/patchs/imx_patch.log
STOP
AT_JOB=$(tail -1 $AT_OUT | awk '{print $2}')
#\rm $AT_OUT
printf "\n"
echo "Pour supprimer le job, executez a nouveau : imx_patch.ksh"
echo "Pour controler l'avancement de l'installation, tapez : tail -f imx_patch.log"
echo "Fichier de logs: $imxHome/patchs/imx_patch.log"
echo "AT_JOB=$AT_JOB" >> $CONF

