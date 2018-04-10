
export JAVA_OPTS="-Xmx230G"

mkdir results/upheno
echo "Processing obofoundry_core_fixed_merged_upheno_merged.owl" 
groovy ../quick_repair.groovy combos/obofoundry_core_fixed_merged_upheno_merged.owl results/upheno > results/upheno/out.log

mkdir results/omrse
echo "Processing obofoundry_core_fixed_merged_omrse_merged.owl" 
groovy ../quick_repair.groovy combos/obofoundry_core_fixed_merged_omrse_merged.owl results/omrse > results/omrse/out.log
