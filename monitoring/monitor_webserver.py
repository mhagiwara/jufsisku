import re
import argparse
import urllib
import smtplib

def send_gmail(gmail_user, gmail_password, recipient, subject, message):
    """Send an email from Gmail's SMTP server.

    Args:
        gmail_user (str): Gmail account email address (e.g., example@gmail.com)
        gmail_password (str): Gmail application password.
        recipient (str): Recipient email address.
        subject (str): Subject line of the email.
        message (str): Message body of the email."""

    try:
        server = smtplib.SMTP('smtp.gmail.com', 587)
        server.ehlo()
        server.starttls()
        server.login(gmail_user, gmail_password)
        msg = "\r\n".join([
            "From: %s" % gmail_user,
            "To: %s" % recipient,
            "Subject: %s" % subject,
            "",
            message
        ])
        server.sendmail(gmail_user, recipient, msg)
        server.quit()
    except smtplib.SMTPException as e:
        print "Failed to send an email. SMTPException: %s %s" % (e.smtp_code, e.smtp_error)


def run_single_test(urlstr, condition):
    """Run single test suite on urlstr, check condition, and return result.

    Args:
        urlstr (str): URL to retrieve HTML.
        condition (callable with one argument): condition to check.
            This is called with a single argument and returns boolean.

    Returns:
        A tuple (result, message) where result is boolean whether the test was successful,
        message (str) is additional (normally error) message."""

    try:
        url = urllib.urlopen(urlstr)
        html = url.read()
        return (condition(html), "")
    except IOError as e:
        return (False, "Failed to open a URL %s: %s" % (urlstr, e))


def run_tests():
    """Run test suites and returns their results.

    Returns:
        list of result tuples returned by run_single_test()."""

    results = []

    # test case 1 -> <title> contains 'lojbo jufsisku'
    res = run_single_test('http://lojban.lilyx.net/jufsisku/',
                          lambda html: 'lojbo jufsisku'
                              in re.search('<title>(.*)</title>', html).group(1))
    results.append(res)

    # test case 2 -> searching by 'klama' returns jbovlaste entry
    res = run_single_test('http://lojban.lilyx.net/jufsisku/?q=klama',
                          lambda html: 'jbovlaste:klama' in html)
    results.append(res)

    return results


def format_report_mail(results):
    """Format results of tests into a mail message."""

    msg = ""
    for i, result in enumerate(results):
        msg += "Test case %d:\nResult: %s\n\n" % (i+1, result)
    return msg


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--gmail_user', help='Gmail account address')
    parser.add_argument('--gmail_password', help='Gmail application password.')
    args = parser.parse_args()

    # run tests
    results = run_tests()

    # format results into a mail body
    msg = format_report_mail(results)

    # send report email
    subject = 'jufsisku webserver test results'
    send_gmail(args.gmail_user, args.gmail_password, args.gmail_user, subject, msg)


if __name__ == '__main__':
    main()
