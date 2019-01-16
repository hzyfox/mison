stty -tostop
sbt compile
nohup sbt -no-colors 'benchmarks/jmh:run   .*' > result/result_fork_1_iter_5-$1 2>&1  &
