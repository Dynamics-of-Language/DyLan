#!/bin/bash -l

################# Part-1 Slurm directives ####################
## Working dir
#SBATCH -D /users/aa2070/archive/class2BDS2/s48
## Environment variables
#SBATCH --export=ALL
## Output and Error Files
#SBATCH -o job-%j.output
#SBATCH -e job-%j.error
## Job name
#SBATCH -J BDS48
## Run time: "hours:minutes:seconds", "days-hours"
#SBATCH --time=150:05:00
## Memory limit (in megabytes). Total --mem or amount per cpu --mem-per-cpu
#SBATCH --mem-per-cpu=32GB
## GPU requirements
##SBATCH --gres gpu:1
## Specify partition
##SBATCH -p gpu

################# Part-2 Shell script ####################
#===============================
#  Activate Flight Environment
#-------------------------------
source "${flight_ROOT:-/opt/flight}"/etc/setup.sh

#==============================
#  Activate Package Ecosystem
#------------------------------
# e.g.:
# Load the OpenMPI module for access to `mpirun` command
flight env activate gridware

# Print GPU information

# Run your script
java -jar DyLanGitCursor.jar <<EOF