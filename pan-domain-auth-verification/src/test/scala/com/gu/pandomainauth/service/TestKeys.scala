package com.gu.pandomainauth.service

import com.gu.pandomainauth.{PrivateKey, PublicKey}

object TestKeys {

  /**
   * A test public/private key-pair
   */
  val testPublicKey = PublicKey(
                      """MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA5AGsiD19GMj8p8jFLRAg
                        |k0z8SFrOU7J3VBCsSn6ByS9tMpkvI9PFWwcmwxgGXAbWkPWOfyC0nNyQPx8MhgRt
                        |zqS+X6j07juaaLnkHh8KmdLYyE7JGH9AfTI2gNI2qvSFhlvYqX8EVVSmooMz6zBu
                        |TIrn9aT9eJRsqBtNw5NKp2lB7FIBUs6by9apZxXXJbwNwe+sIAIpin2Mhg9mQjjS
                        |MSKet0NY3THlUQwGSCvMVs+CD9MyMDyJxsNR0TJ2QuOF88Flz2dvguJ3RYetAq5u
                        |DAKHMbATSJIQXTlw7aGfKTUi8c3LPeLZgJ9KScbwzOIOcWXm3F7zFlUWWdbJ58DX
                        |DMr/Itaufgn3ffXwp5AtmntzpQ74ObHqKbNxbyRg6Chxu38cY3MdjNFouSGhNSUQ
                        |nQqBFUsTI6igRtgDwZaAbGYoWT6vdoWCx51+c3FU5yL4A5Ew/uVFNrG5bHmWbvr/
                        |k5VsihN5iSGSlTqy1/sBppxsd+1955ukKmJsbdbHZoVceqKpVAIlXt8uFHybRp1y
                        |q19rXt5nBnpqVND80oPPn1wc1WrSy1sm8aQwtKSBoNJgvO6diuKPtX2BnQxzKjEw
                        |p2RyzmRIBIw16kjPNLKGgakrJOZP51gFdOA1qjUA44w0V2mxbszq40aMYFsI5Kyd
                        |qqXkOlqIoeN8DHVaNBPiSakCAwEAAQ==""".stripMargin)
  val testPrivateKey = PrivateKey(
                       """MIIJKQIBAAKCAgEA5AGsiD19GMj8p8jFLRAgk0z8SFrOU7J3VBCsSn6ByS9tMpkv
                         |I9PFWwcmwxgGXAbWkPWOfyC0nNyQPx8MhgRtzqS+X6j07juaaLnkHh8KmdLYyE7J
                         |GH9AfTI2gNI2qvSFhlvYqX8EVVSmooMz6zBuTIrn9aT9eJRsqBtNw5NKp2lB7FIB
                         |Us6by9apZxXXJbwNwe+sIAIpin2Mhg9mQjjSMSKet0NY3THlUQwGSCvMVs+CD9My
                         |MDyJxsNR0TJ2QuOF88Flz2dvguJ3RYetAq5uDAKHMbATSJIQXTlw7aGfKTUi8c3L
                         |PeLZgJ9KScbwzOIOcWXm3F7zFlUWWdbJ58DXDMr/Itaufgn3ffXwp5AtmntzpQ74
                         |ObHqKbNxbyRg6Chxu38cY3MdjNFouSGhNSUQnQqBFUsTI6igRtgDwZaAbGYoWT6v
                         |doWCx51+c3FU5yL4A5Ew/uVFNrG5bHmWbvr/k5VsihN5iSGSlTqy1/sBppxsd+19
                         |55ukKmJsbdbHZoVceqKpVAIlXt8uFHybRp1yq19rXt5nBnpqVND80oPPn1wc1WrS
                         |y1sm8aQwtKSBoNJgvO6diuKPtX2BnQxzKjEwp2RyzmRIBIw16kjPNLKGgakrJOZP
                         |51gFdOA1qjUA44w0V2mxbszq40aMYFsI5KydqqXkOlqIoeN8DHVaNBPiSakCAwEA
                         |AQKCAgEAgIwlDsbEAbZHI/2AlVBzXTjZP53al7uVpVKlIHbEi33urclJl4Rsz05v
                         |3yxrYXUFgcK/7RKuYYXE2aGSuPhERG4DWwFx3eLCwmqTnxvXKTSDviMVI3eTD0LB
                         |Ec3yvl2P7llYBO+0rLtGG3enTOEIPqVN8+NqeJIN1JVYOXyQaWRho2/0iRAtLDZB
                         |WfopS4ADraSHwaAWdZslH+wMtic88jDXlPEmZ8cax5/k5smysfraFDv5HtLyxpoW
                         |RcCShlGqkuwlj1J8vykZH51HwKxBJQpD2JKxvIeFLFoXPhKSHnRz2CozfRepkue7
                         |xW6hEpIcnHhcOUBbAutD57C+Mqg5VU+VSslevqHki9eWzzFuMfpVmMQZWPwc9LQo
                         |XsLOMTLe7fo5vQtAii8w3JfqOqo4Xy4KlEEWDle4TpSywcRqWXuIEJCF0eLxJMCT
                         |7vLfsPpVZhLAoQujRFy7+f/+mNoYkrRAASQFr5g5OiXMjvtaHmEQC4J1YLPn4Kbj
                         |LoWyof4nKg5+IeVAfrJhRJdyeA5ZhRvuO/uk4XaKHyR4pUB2I0HB4YN1nWnIve5l
                         |G3D+P3sBVNN+vXbEJAVZHOoR6k1wTXt1DC2X8YoqeKFU/LS7prnBZbqbH4cFSAnw
                         |Ej81uwukxtTGJ9ZXl2r22N39aAMp7HgVra9FOOt3xU+Tn5FnvVUCggEBAPf3W/tU
                         |jqrZEYkiwbhkKc2ZrAE2i/Ue5qK/lJwEGvRquoIWNb06+a8OtqlGhTYVrYiT0wPX
                         |+qKHFo/uIHwKP+tXeN8mJmjl5T8Ob9BqcrH6HT8FwP6oIMoyEcphA4OIKX+/ZhNq
                         |M348ihEbhm1QXNisU02Eaf5OVFFocEZ4ihe6sIr4NsZRBoYyiC5jb7CGyAP0JZAq
                         |g0ELTnUa1vFLzHr0ZkIKAKI4DxsImvs553tQA+OiXmlXNQV2KXm4KB5HIq3/Ewlw
                         |ogeTOyORIcdkaeSj43j1juxvBi3ripobWIExKTI6fXPwV8bHxBTlPUF/OgQztEj9
                         |RitOyqq3Ep/mgFsCggEBAOtkxKRexyzS57IFqN7oGkxX13d03LWU9TkbpR6wmC3h
                         |g9TZEL/ivrPHo+s6v/oLc0UxiXMmBL2w/wbtRIt1C4ywBV/DKa5jOkd8V9v7yLLE
                         |oEs8Ttpa4IqUul+qbAJbyPil55Ot1vq0kO4j0o/sPMeJmSMxkY9HxH2V+jfUlu4l
                         |po1H0Ai3J+QHwZgoh/S/IkrZT4+Jw6fsumQdgAs9pbFZvgRMUqRwClwSpNbypyh1
                         |dP4f+mCO36RUoM0Kq0dLTOmYKEegu24uJEziBaGyI7PfF2KbnhlQOc9XAJBGhObc
                         |ZETNUNUQiZxW1HxcDTw6O4utQUwB5y9LDQrvEpdfPUsCggEAQ8xnYvXAuGf3inZt
                         |XzxLzcJprD88zz2us+sL+AiLUi7oZe8qrF22rS2ptejpPVt6AjDg7lkcV1n2Sk5C
                         |pYer6G4XR3RH5kCNiMsquWKRj6YiXvv0w+w2YvKDuMED0+/j4OD1SPOttqNY4MqU
                         |28iwSjH76OL+lCDYnxWeNTZ6E3jhEm5Bk8HNtd2VLJHVlP+VKreTmbJ2Bs/PmOdI
                         |w9sEws2WNBJGEBSh1LNqpKw1ZZsAJTpc9BJ7Fc40EurOVVD/43EtxFP+iXGxJs1k
                         |3f7PkUtaQ9VoSa8lkmKnl/FIJrLJX8lsScn6RgiSzV/Met8Nft6gOaC+kdLOSRUb
                         |S4sbpwKCAQEApN6JVE8F1gFKQsfwBWAvuzTzNznktFHGh98zAZHSpKAFQ/lc3+ki
                         |hKzZxJH7kFigvd/6U2pXe70SgnmAvuSV2E4ZFX+OHBRn51XR+8QYSA2r78YnvIxW
                         |JPoSp5qn1+cNlgotbqQdpK7/q6vveNqPn3piheHb/6zWlNeMz7nYlKhORTn9HX2b
                         |NP2xvK72upd8n0whi3mEToNh8WkwG69r9BHVIxGJ+J54wxBT4DFInu/Vp4AiT6sC
                         |PQkBL1u/p0m4bwwhejm7zWoIBslwBFbO5tbkfC5ex4AHXY+kV2jtAP1wRThA21qf
                         |555xaUDYDE0FE8mfqGrUz60TT4svTFJqjQKCAQB7fr1q88w6MaTVx67g3s0KtgxX
                         |M9WOjP7RJy5cyfBTLvvsfE69Pprse2gthNN/808hBuftX0DttspwpcLaRX2PGuN0
                         |YKkd8Tsi/AKIOmPmGQo7OueR/ogeicTLPhidoG9409x1rmKyEsFwjbHTXbQPqhYa
                         |wXED1ryRmJJQ0YvAm53fex+Hvh6+8gCAP0Rb13UaQnnD53+OPYpzqcoGL5idrhnb
                         |Uk5PRs9HcBnKK3+FLkzvu2lJsUd9LxZCNjabNslaiaJAc98DJCaHgplPhoyRpc5l
                         |CPdFb0yoYfBZjP3DRXJj2koucqru+2tu2WX+HT216OrB8b20CmUlUynwJvtj""".stripMargin)

  /**
   * A valid private key that does not match the public key (useful to test things fail with the wrong key)
   */
  val testINCORRECTPrivateKey = PrivateKey(
                                """MIIEpQIBAAKCAQEA2rEzkKiGmC1Dy+MlBESQDhaokUKGKnbyB+8AoZ3dWvMKkUiC
                                  |u6LoXRPePT9ncKVtJk0fnv08Ca+eP4JOaVJMxZoHnDzhwMeRzmofHlqE6IMsU0vq
                                  |ZHZLV0+WDRCjlScZ5xq/Pvi2HUXTDrGp+DmLzibkerPBNd6f2sMeGpuGXD6ha0no
                                  |PyWjXDmhKioaadoYvgyHZ/i5gbOVFRmBzNadsgemAo8FGtAPIE3cd0AKfgAuuGyW
                                  |dPBuXU2y0xb+B59rrp8bPMg3qOAp9NxgSWhSCgA2m4mKmn9VQRoRCiss/bOcwPPN
                                  |/cSH9YRFEgyFRxObHRn2IA3oaznih4cw5Olb4wIDAQABAoIBAQCyVdyJ85PGlur5
                                  |YeK2xz597qZJxmjoShT2uUTXhoeS+iy86teJMcCw3lnVslWk+5G1sSC/IxyJfYfb
                                  |nGRa6L0oSoI4O0GRVm0lWU7FDuXhQc6sonSitgCcU482WkI7iCZMWnhoXqML3fvN
                                  |uL21aSAD3Z1mHh3J0yCiYYi0A+2eXAYFihlx+sHIMlZrOXM3/nQ0b000dvVdP32d
                                  |TwlmnRljgPbL/5pE1xzUmMPFlQEc79/AQnuRjyKsLzMZ56OoSBh1BN3gjcP/nPMU
                                  |CCKYVBh0FF+pjU9uNnGDQwptpv2UDPfSQ2oNczeomyGiytrV8xkHeEUhnkag9KnV
                                  |jW11z+85AoGBAPnm1OqUU7Y5cXYxNjJxl8FdMypLCKDUwI8ER6r06t7+jw6FJa9w
                                  |azGZskp5hjAFRHxbSP83E4Mh9RDkkUi74/9baEoC41NV5Y/oAXaOxbT4eGk2cAoT
                                  |p8lOnwuoS/OqXx1rwkqZwo+Pa2KgPBdovgM6NoyFgPKVFxLNXF7ru/+tAoGBAOAH
                                  |ZmP9Qe5Fd4DqJt3NjT5zbrQDJGLw6t3h0Mve8hUQiEwWm67ALYMOPiQ0OX983zeZ
                                  |6RldqotnQNuPwBtkunJ8EVlnP7wEm4YHCLkWb2S9gd0gSgundW5sD74HmrgIzCkm
                                  |7w2dqlpn+J3vrEtHngyZsYK5SrdUJO856OSv8/vPAoGBANG5+OyZ6RtgYCmaKWry
                                  |TDL6ncsbx7T6eFROejsuasMVDkhYVAVFqqCo5al9DbHVaeGX1s5hkZ2W7Q3tVUSe
                                  |cz1GoGKkw9WXnB5GzpuKPMChWqSqDCNY1ZKryezFpGBtln+hhSCIpHFJIw1VfrkC
                                  |rZ0VyNvr2wk7OG8OLDx5SIDNAoGAGUycooQSMv9DTJqkwv2YisnsYYXNUSMV0vLS
                                  |aoOF4R0Z10XiVOy89wTfvaTsLVqDtrq52TR7svx5FZJ5Rl6ss4sRRoBjcS8wBQW8
                                  |VXKUJ9NC3B7uXbyOhWbMrFAdh9TugWM1MdtxRn5hVCyhz2qKDbA5nKKiLmvhM+bd
                                  |Rx4btn8CgYEA8tCxTDjHi/TA3KV5+rHjCo0IYRIvywg6bqy0dDwcZjQ288bm3qPD
                                  |o9PbUxP5TRl8LwEorThRXatdFvOwKC5LDtzyXWzkHbpV2Yu+e5QGZw2HGOKZ3AjC
                                  |JYA7mT0eNFmOa/d1W8NdXhh6YKYiV4y8q07WVtLUnBowMoa8tgB/53w=""".stripMargin)
}
