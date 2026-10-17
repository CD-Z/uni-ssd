for f in ex1-1.xml ex1-2.xml ex1-3.xml
    echo "--- Validating $f ---"
    xmllint --noout --dtdvalid ex1.dtd $f && echo "$f is valid"
end
