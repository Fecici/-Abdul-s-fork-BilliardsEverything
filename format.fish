# the directories to format
set dirs src test bench profile

for dir in $dirs
    for file in $dir/**.{cpp,hpp}
        clang-format -i -style=file $file
    end
end
