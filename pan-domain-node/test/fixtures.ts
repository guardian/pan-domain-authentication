import { base64ToPEM } from '../src/utils';

// The private key is not required by the node module as it can only verify cookies, not generate them
// I've included it here however so we can use it to generate new cookie data for future tests
export const encodedPrivateKey = "MIIJKAIBAAKCAgEAur0hOjhB2QWjwOopCR+Qo27AYv97BJkVaKWPXpj9RfvY1wtpIratDN6tkXN9WCRPzVX8+5qaW034Kvf9WwBZD1ntS8iHYwY1YUaU8Mrp2sRT3K0RqyBlTswIH3HIqpASqv7ZtwDHdhk7Cbd13P5aomJSOjYFhCDUi3sRbjJP1kb6uQLdkZj8fIU518HzSR7Kw7p2mbDqSrGbnaeHWd0Tr3BvDHp9Pi0KpSAVm2qWAXix+BcjMA4ar7kLU1Pre0lt4K4DlSvq5XoHdX9/yvS6KGf+8pXDR9bY6dgRPSG4mzKpiKfkv1eXE8WKs0q3217QZItaSjocw4d0o47vSN+/MAh9V5Zewyb6ogs8JicX3Y3FPG29I1g8iLf3kBZ7V4mUimGuOq/L+1YVvyOTb2zWWMjNmECO2lrxXJc5LWRs5FmSJyCdilRktDE0WTGUo89O+DcF2752qtpUmlV2fllU1LXIAn0TJKiAZspKakamifrgYFIzZK4oZ8wDeFesQB/a/U7wtyv85vknzCtMLI28dnpGQ/ZFHNqWYVaHoHsnEmWion7lgMctnpY5pwKFfUSfZecl2Xqwjk1HZj71A9TFNQj+/x4z957cNtx+utAkGinK3eZF+H1o5YnSgjg4hN41kbXttk8nADerPdF7hDS6np7xzUl6qOicJhEOJ5x0c18CAwEAAQKCAgAFEDXDb10Rtl5vT6oXLjzswYcD6Ct8v23eLYcKqJlNeXuysQODxnJAxBTuubPvXOSxC6DVbaa7zQxqldjPy92eVfDiOii5naR648AMG2Rl4ybm9+Zfvnwgu9WIjLxFK6zl6A0dMi82W47HP6s5d8gbWREjtO1HXOCGe6rIUyLpC3mm5JX/aaeG9NHRsNeY5vXWgsrOdgaUSeaPSsiXvi/XdPP94aBdvDjqq0kKssQofA5PTMlOd0Nv+lN9Sew7po0NJ4q/U7aFzF5BaFidty8JA3DdQQRPgVrWVF57StvHkYMZSnwgWA6noZaWL/N2RkbeQw0KsDKxdo3KFYkVb8OuTN68b3J5sKIo48LaDbfWbuThcdUFdpKXUd6lvJ2vCnbN6e1fzJdjpCNg9mb2g8KnSW9xqXek8cx0b+LNh+p/YDUI/BZDfsXjeqODiGEruYWfaYiL4STOOXIq5SoyP6XEDbX41UHcb/P3pJExBB7R6fpUFREUd0tUAQAQ93YUitWIs8U1tVAB9/qaUO6BFgOghrbswUiX4twVdmqhrBVOcYUImu7LUdcpMLIdR/gyy47w8xEhiv/oudWVx7rkJS4+l949KtUmyd5x+MNT7nAF99eSIv3iNpL6eS7OWiak5gfoVATy0shNd48H62mGtYjfoGzndGE26tNCHXoAkTrbQQKCAQEA47QXH0Y5xbnq8rlqukwVt+EDMJUtZBqfxGDnkDmtfGefxiyE1VLb+Q0Z1es3lxjcmb1Qupl9tjrWeejK2QymSOzPaluOTpIFOZbFqzNha68nONoiWjqwkCg7VvTo0mRKLwSEnCrCOB8HiLKQAhirdPwtW/8PLd+B8HZSB3UKFylbT+ghHIz5b4P81GksRGbsb6g3+ipz53mvtJpe77J7Y84wPQppj6Ry0WjsQr4iv2hF5PWNMhHVDj8eO80DJvWRCFVTWqTE3WYxZJ/5jdeOdVTwtrYghP72X0zAnVpQCVzcn8tLghKsfePhCw3zNtFE3dk4XHQD4uhPU88GtxJ3vwKCAQEA0fHVZmW8EwxYUEzhFl+2J4evszh8R2Sga+LHv4hy2qGydiciVnv4Y/IoScc0IQ+YHoj9fNad+/UoR8ctSBgrPDzeiY/7ldSp/j5Tbqb4h3Ioaqf9uNg7qHpnciz1o8AI8fFelXZ5hA8GuXo8VGy60xMCyapjkLzTXshdRPjUzcLX1PrUy+me4ZR/KdIkqOOmzgvBVII3gb0iN70gTqn2HSIUC3yMiXA+X6OkHoDL7tn8tkF/aD+uenxOvOadsoJPtaFPaMD0X5vJ8LIjsBThAncVApx93XJIdkJm2ffXq2JhQJfZ6ZXypa6ooepzmB58kxA+TZiJpQLLQJ17xC/sYQKCAQBwuzJPW3cyuw7kyINcZFrERHRN0y07yCqdENTUBJotYygo9tV0v6cEMEZAMEm/VqGww5d6Ko+gbpTMmkIDH04cAJHXuChGIejQUCLg1Xk/1OF4NhaX0UKkvCZUsL+rmddYW8ZDgq/RFRunw6+kOg54xni2eRpMvcEZCZsm8fzi5qi8cNIjzm+XlCLSDpfJ7aLUzNWZ1va2/PnOUjb6OMT57pTXQ5ZrdSEbJ/UAPh354WfpKOCUj1uJyBnxxVfwK9d35rZzw+trKTL+/GySmst+r2TVMGn9LjVPjTI3NQU2/XCE9CMX7KLVWMKLtIZa91Q++VH8A7wA1L6hYXeTn2MFAoIBAEb5xvdTNX4LEmAzXXU+7kn26UNhuUI5lrJifL0X2BxpxfeDy2wJhTPkzhIDMnBq4TaRgYEO3WIsw21gvMI+yX8X5PQEpT1GJCI71+D0udiwk1Fbcb9n+uM+XnKPGIw/g8annx5Qa0xl+BQEaxjvmUl6h9q9q+Nmst68RivnI6pcULNECWTWmkwQ89yjmpkuPVozRyzWyQUnd8X4Pk/ZzcaTmss3VBuywqN6oyVczZT2RSUoh3Yq8UWfeM8L+Aw9Wc1Bt6LmeLdJ579jugTxShCXSZcUaMjQtgak9DiEPXlHTTGVJKp/cwToQ0JaDLJEvEDLoQSCqSYMB8LUet8chIECggEBANxvUJxCLbx4CZrUJC5O2w01GMDFdTfYOUcK75pFAbXkpBPXDLsuz2dK1y8ANk3ibWWrlV6YKDUwl+gWDHmJOcvKqKKAeUnrhIMmJGaO6C++5DxPE/n7g5M86GA0bu/+B+32wL/65B8HoJrkHnSMJp9GcCsVZA3+2xcJfo+xAiXeiRobRIxCQMYCDDM7Hr7X5jGa7l9bQr4GuWRKRYTroE9LCDnH/LLUN+0ny3UXrSjtTUVL4mVAJT0Ws2H1zUzVDbu7ZQgA0u3GjtdFvAnS/E+ln8DS3Q1DeD6Zsf0hrrJbtwU4zZIU445SZ+IUaTjueB9v/skukoIQi/0Mj+gpZ1c=";
export const privateKey = base64ToPEM(encodedPrivateKey);

export const encodedPublicKey = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAur0hOjhB2QWjwOopCR+Qo27AYv97BJkVaKWPXpj9RfvY1wtpIratDN6tkXN9WCRPzVX8+5qaW034Kvf9WwBZD1ntS8iHYwY1YUaU8Mrp2sRT3K0RqyBlTswIH3HIqpASqv7ZtwDHdhk7Cbd13P5aomJSOjYFhCDUi3sRbjJP1kb6uQLdkZj8fIU518HzSR7Kw7p2mbDqSrGbnaeHWd0Tr3BvDHp9Pi0KpSAVm2qWAXix+BcjMA4ar7kLU1Pre0lt4K4DlSvq5XoHdX9/yvS6KGf+8pXDR9bY6dgRPSG4mzKpiKfkv1eXE8WKs0q3217QZItaSjocw4d0o47vSN+/MAh9V5Zewyb6ogs8JicX3Y3FPG29I1g8iLf3kBZ7V4mUimGuOq/L+1YVvyOTb2zWWMjNmECO2lrxXJc5LWRs5FmSJyCdilRktDE0WTGUo89O+DcF2752qtpUmlV2fllU1LXIAn0TJKiAZspKakamifrgYFIzZK4oZ8wDeFesQB/a/U7wtyv85vknzCtMLI28dnpGQ/ZFHNqWYVaHoHsnEmWion7lgMctnpY5pwKFfUSfZecl2Xqwjk1HZj71A9TFNQj+/x4z957cNtx+utAkGinK3eZF+H1o5YnSgjg4hN41kbXttk8nADerPdF7hDS6np7xzUl6qOicJhEOJ5x0c18CAwEAAQ==";
export const publicKey = base64ToPEM(encodedPublicKey);

// The comments above each fixture are the Scala case classe representation used to generate the encoded cookie

/*
    val user = AuthenticatedUser(
        User("Test", "User", "test.user@guardian.co.uk", None),
        "test",
        Set("test"),
        expires = 1234L,
        multiFactor = true
    )
*/
export const sampleCookie = "Zmlyc3ROYW1lPVRlc3QmbGFzdE5hbWU9VXNlciZlbWFpbD10ZXN0LnVzZXJAZ3VhcmRpYW4uY28udWsmc3lzdGVtPXRlc3QmYXV0aGVkSW49dGVzdCZleHBpcmVzPTEyMzQmbXVsdGlmYWN0b3I9dHJ1ZQ==.tDQmAb1HaXzrEaLQKK3xLtICoQm6R/8eFbjISe7IHggNOzdnypnPZKXiWXqYbC22ROL0i0e+iUWHc7icEfAO6LXv5PwTQuvI8i/q8IE5Vp+/8S319dvNOyivXiLAdTBIvqjPF0ue4Cj0j3uWMU/n0A54qtX7PDJJSDdYmRRbDN/FsmnEslqBGCDSzMTkNhPBBXcndn2YXLTnMv0vmZbRJxyygRfV3rvy6vzU4NqaIMkvSu3vd0Pqj75B9IAG0epSvC36ysAUrvhYSWcWMj8sv0oh0lWuo1Ilu5Vu+8oJ5eEr82ZqALT9o+8dP7h4NGtRNkgP81y2RtK68eDWCpOKp2JXwrvb+F7QInEsZcatEr2aKioCsaszNt5tH0FtVVuwZ+Ul6Fsu2hZG84a1pTq34zsxh0WafYTciAOmoMvQQcZSKpJc9GfxtBQo6lq9X7TPIG4flNyWS7GD61p9q8pqG5wgtandG9lIueEJLSsMOPkhGfBs/WOfLs1ftquwqt14q985twBw3k6yvMBcVB9pPdm0h/6vcLF4GwZekHiFhOmyguU7FLaDfHL+TIHSPlCy1iCtE9BmIU0g0e2RRN4bDMT6PrLPN+fx34/2dlonGHnDyQw794G0NOBKUtxsaw6dx60yv/J19dau91qyGfTMb4nXmehm8/B78y/RecveWgI=";

/*
    val user = AuthenticatedUser(
        User("Test", "User", "test.user@guardian.co.uk", None),
        "test",
        Set("test"),
        expires = 1234L,
        multiFactor = false
    )
*/
export const sampleCookieWithoutMultifactor =
"Zmlyc3ROYW1lPVRlc3QmbGFzdE5hbWU9VXNlciZlbWFpbD10ZXN0LnVzZXJAZ3VhcmRpYW4uY28udWsmc3lzdGVtPXRlc3QmYXV0aGVkSW49dGVzdCZleHBpcmVzPTEyMzQmbXVsdGlmYWN0b3I9ZmFsc2U=.gpjoR50kXKjImTPddWTv2YR7B1GjghIai5bxt7COOo3G+MPtyPs0H6D72JRoOKVjAwnI82fOnUIjka7EJPb9GdUGw3f+/wYn56MM6gAt7lxj1mVeCG0vY01s2Za8dprGdQYkh7xFQgbfRK01HB3UFSMBw4MtnQFbV0wtEJ5aNhHXc7XJZPQm5UBo8LLiqsbdIHg4dfh61fD58HroPRAoh6IGXeqen+E4KoEdItbyPxjFrlSdXiYtnySf+4h4i4naANkFjy3l76o+OhkFp2f+6j0IeqwpZxwJe8qTjt+8hhcS/QxqLaBeCDRvbVMxSJIeOJqD2RBKf2HDXXBhGVOWhR19YX0v0Olci4265cNqUaSDw3LLvhPJ0cR9bYdOLseaQOqKmUHQZunapwFthltPx5NH4e+UvulxnzDWfDxTzAmrln2bdMNp/H2PjGY4PZ2BEZGKX6xf62eYxlIHeBGQlibr0R0whbPq0NONx2rqqS9vDNpGsQRR7eMNEMwr2o8XZG6qWA+Rq1OMMwRVBG/9s9CG0XcMTh36J5RwRy9ilCQhMrFH0B7MpwRHOcjcBwxsyZanFRaRGrb2P9w88ozBEUKkWejnLfZmhRipnrJJuaNweoJfXhhK9vr/LrE39tmgdKeSxNIb+ZUO5CSVvQ74QVpS/EGd5xslv0p8+2fOQts=";

/*
    val user = AuthenticatedUser(
        User("Test", "User", "test.user@bbc.co.uk", None),
        "test",
        Set("test"),
        expires = 1234L,
        multiFactor = true
    )
*/
export const sampleNonGuardianCookie =
"Zmlyc3ROYW1lPVRlc3QmbGFzdE5hbWU9VXNlciZlbWFpbD10ZXN0LnVzZXJAYmJjLmNvLnVrJnN5c3RlbT10ZXN0JmF1dGhlZEluPXRlc3QmZXhwaXJlcz0xMjM0Jm11bHRpZmFjdG9yPWZhbHNl.mX4eQF7RXNJXnI9szVwGaMN5XL8ruTiZyDQ8/fEjO+TcC+q/y1rPCjRWAjhmBD+VjVES3IcCdlC4bVQ+T9BM9eLtZYnDGFDAalKP1BnoGYg3nGku0vJi7n+lS/UC16zmJyy19SgkI7GqjoTgaeN7PRgE4kZPWHtXJQa8GyC9Bc5SInvhgW04yIe6Pm9IIVGGNqWo3/qmiTDHxWPHBSxf8gNO3d1VTmp4homkIxKoqaJNloCPSjYvgk1kf8ZJQFPL00HNA+gYNRjQDpDbMAINMS3/BQLdEkRKaTIqmIEmp3+LVoN+8p11K7fd8deOQXsMqnhGCFwxAr3Mk1hUXQsvOl2uHSFg2eRhHbTbILvurNp3hmtF3kld6EmrFbKiag73YsLV6/x03CrhktvUmQlhP9/DdOt+qWEkDCcZP5z4oazJjiiVl21F2TZ1EOD3Yjw9OSVR7tGxe6gPzi/bQQa/Yl3Gd5FtR53QMbnKMBB3NIWxFl7JeVHtvZo+4SSjVRTPxtBnnwnYW+jQulhMCYuGRIKUJ4v4fbCsUXhBguYGYz6v1vKcllobkyMa8AVX38bLH/+CFY1u8mCdjD81Ojwg5iWPoJu/7z/AZ+cKNRquHuEa3z5R6OZBE4IxDqGFc5U8ZdvBYxtrPR19NzwmDZ0uebtZ047P7xQKHAaS+NTitDo=";
