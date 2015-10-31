import sys
import argparse


def main(source=None, doc_id_prefix=None):
    print '<add>'
    id_num = 0
    for line in sys.stdin:
        arr = line.rstrip().split('\t')
        assert len(arr) >= 2, 'Make sure to have at least jbo_t and eng_t fields'

        jbo_t, eng_t = arr[0:2]

        # Add source field
        if len(arr) >= 3:
            src_t = arr[2]
        elif source:
            src_t = source
        else:
            assert False, 'Specify either --source option or third column.'

        # Add ID field
        if len(arr) >= 4:
            doc_id = arr[3]
        elif doc_id_prefix:
            doc_id = '%s:%d' % (doc_id_prefix, id_num)
            id_num += 1
        else:
            assert False, 'Specify either --doc_id_prefix option or fourth column.'

        print '''<doc>
<field name="id">%s</field>
<field name="jbo_t">%s</field>
<field name="eng_t">%s</field>
<field name="src_t">%s</field>
</doc>''' % (doc_id, jbo_t, eng_t, src_t)

    print '</add>'

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--source', help='Specify src_t values to override')
    parser.add_argument('--doc_id_prefix', help='Prefix for doc_id')
    args = parser.parse_args()

    main(source=args.source, doc_id_prefix=args.doc_id_prefix)
