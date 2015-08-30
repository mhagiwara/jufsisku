import sys

print '<add>'
for line in sys.stdin:
    arr = line.rstrip().split('\t')
    assert len(arr) == 6
    sent1_id, sent1_lang, sent1_text, sent2_id, sent2_lang, sent2_text = arr
    assert sent1_lang == 'jbo' and sent2_lang == 'eng'

    doc_id = 'Tatoeba:%s' % sent1_id

    print '''<doc>
<field name="id">%s</field>
<field name="jbo_t">%s</field>
<field name="eng_t">%s</field>
<field name="src_t">http://tatoeba.org/eng/sentences/show/%s</field>
</doc>''' % (doc_id, sent1_text, sent2_text, sent1_id)

print '</add>'
