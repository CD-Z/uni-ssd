for f in ex1-1-nv.xml ex1-2-nv.xml ex1-3-nv.xml
    echo "--- Validating $f ---"
    xmllint --noout --dtdvalid ex1.dtd $f
end
