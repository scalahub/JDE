package web

import jde.compiler.TxBuilder.compileFromSource

import javax.servlet.http.{HttpServlet, HttpServletRequest => HReq, HttpServletResponse => HResp}

class JDEServlet extends HttpServlet {
  override def doGet(hReq: HReq, hResp: HResp) = doPost(hReq, hResp)
  override def doPost(hReq: HReq, hResp: HResp) = {
    val resp = compileFromSource(scala.io.Source.fromInputStream(hReq.getInputStream))
    hResp.getWriter.print(resp)
  }
}