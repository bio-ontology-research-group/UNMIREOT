export JAVA_OPTS="-Xmx230G"

mkdir results/pco
echo "Processing obofoundry_core_fixed_merged_pco_merged.owl" 
groovy ../quick_repair.groovy combos/obofoundry_core_fixed_merged_pco_merged.owl results/pco > results/pco/out.log

mkdir results/ontoneo
echo "Processing obofoundry_core_fixed_merged_ontoneo_merged.owl" 
groovy ../quick_repair.groovy combos/obofoundry_core_fixed_merged_ontoneo_merged.owl results/ontoneo > results/ontoneo/out.log

mkdir results/oostt
echo "Processing obofoundry_core_fixed_merged_oostt_merged.owl" 
groovy ../quick_repair.groovy combos/obofoundry_core_fixed_merged_oostt_merged.owl results/oostt > results/oostt/out.log

mkdir results/peco
echo "Processing obofoundry_core_fixed_merged_peco_merged.owl" 
groovy ../quick_repair.groovy combos/obofoundry_core_fixed_merged_peco_merged.owl results/peco > results/peco/out.log

mkdir results/ppo
echo "Processing obofoundry_core_fixed_merged_ppo_merged.owl" 
groovy ../quick_repair.groovy combos/obofoundry_core_fixed_merged_ppo_merged.owl results/ppo > results/ppo/out.log

mkdir results/ro
echo "Processing obofoundry_core_fixed_merged_ro_merged.owl" 
groovy ../quick_repair.groovy combos/obofoundry_core_fixed_merged_ro_merged.owl results/ro > results/ro/out.log

